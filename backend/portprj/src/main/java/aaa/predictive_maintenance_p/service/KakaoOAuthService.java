package aaa.predictive_maintenance_p.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KakaoOAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration ACCESS_EXPIRY_MARGIN = Duration.ofMinutes(2);

    private final KakaoTokenStore tokenStore;
    private final RestClient authClient;
    private final RestClient apiClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();

    @Value("${kakao.oauth.client-id:}")
    private String clientId;

    @Value("${kakao.oauth.client-secret:}")
    private String clientSecret;

    @Value("${kakao.oauth.redirect-uri:}")
    private String redirectUri;

    @Value("${kakao.oauth.frontend-return-url:http://localhost:5173/admin/predictive-maintenance}")
    private String frontendReturnUrl;

    public KakaoOAuthService(KakaoTokenStore tokenStore, RestClient.Builder restClientBuilder) {
        this.tokenStore = tokenStore;
        this.authClient = restClientBuilder.clone().baseUrl("https://kauth.kakao.com").build();
        this.apiClient = restClientBuilder.clone().baseUrl("https://kapi.kakao.com").build();
    }

    public AuthorizationStart startAuthorization() {
        requireOAuthConfiguration();
        pendingStates.entrySet().removeIf(entry -> entry.getValue().isBefore(Instant.now()));

        byte[] stateBytes = new byte[32];
        secureRandom.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        pendingStates.put(state, Instant.now().plus(STATE_TTL));

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "talk_message")
                .queryParam("prompt", "login")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new AuthorizationStart(authorizationUrl);
    }

    public OAuthResult completeAuthorization(String code, String state) {
        requireOAuthConfiguration();
        Instant expiresAt = pendingStates.remove(state);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("카카오 연결 요청이 만료되었거나 올바르지 않습니다.");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("카카오 인가 코드가 없습니다.");
        }

        Map<?, ?> response = requestToken(tokenRequest(code));
        String accessToken = requiredString(response, "access_token");
        String refreshToken = requiredString(response, "refresh_token");
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(number(response, "expires_in", 21_600));
        Instant refreshExpiresAt = now.plusSeconds(number(response, "refresh_token_expires_in", 5_184_000));
        String userId = tokenOwnerId(accessToken);

        tokenStore.save(new KakaoTokenStore.StoredToken(
                userId, accessToken, refreshToken, accessExpiresAt, refreshExpiresAt
        ));
        return new OAuthResult(userId, frontendReturnUrl);
    }

    public synchronized Optional<KakaoTokenStore.StoredToken> validToken(String userId) {
        Optional<KakaoTokenStore.StoredToken> stored = tokenStore.load(userId);
        if (stored.isEmpty()) return Optional.empty();

        KakaoTokenStore.StoredToken token = stored.get();
        if (token.refreshExpiresAt().isBefore(Instant.now())) {
            tokenStore.delete(userId);
            return Optional.empty();
        }
        if (token.accessExpiresAt().isAfter(Instant.now().plus(ACCESS_EXPIRY_MARGIN))) {
            return Optional.of(token);
        }

        Map<?, ?> response = requestToken(refreshRequest(token.refreshToken()));
        Instant now = Instant.now();
        String nextAccessToken = requiredString(response, "access_token");
        String nextRefreshToken = optionalString(response, "refresh_token").orElse(token.refreshToken());
        Instant nextRefreshExpiresAt = optionalNumber(response, "refresh_token_expires_in")
                .map(now::plusSeconds)
                .orElse(token.refreshExpiresAt());

        KakaoTokenStore.StoredToken refreshed = new KakaoTokenStore.StoredToken(
                token.userId(),
                nextAccessToken,
                nextRefreshToken,
                now.plusSeconds(number(response, "expires_in", 21_600)),
                nextRefreshExpiresAt
        );
        tokenStore.save(refreshed);
        return Optional.of(refreshed);
    }

    public List<KakaoTokenStore.StoredToken> validTokens() {
        List<KakaoTokenStore.StoredToken> valid = new ArrayList<>();
        for (KakaoTokenStore.StoredToken token : tokenStore.loadAll()) {
            try {
                validToken(token.userId()).ifPresent(valid::add);
            } catch (RuntimeException ignored) {
                // 다른 연결 계정의 발송과 갱신은 계속 진행한다.
            }
        }
        return valid;
    }

    public int connectionCount() {
        return tokenStore.loadAll().size();
    }

    @Scheduled(
            fixedDelayString = "${kakao.oauth.refresh-check-ms:21600000}",
            initialDelayString = "${kakao.oauth.refresh-initial-delay-ms:60000}"
    )
    public void keepTokenFresh() {
        validTokens();
    }

    public ConfigStatus status() {
        if (!oauthConfigured()) {
            return new ConfigStatus(false, false, "카카오 OAuth 서버 설정이 필요합니다.", List.of());
        }
        try {
            List<KakaoTokenStore.StoredToken> tokens = tokenStore.loadAll();
            List<ConnectionSummary> connections = new ArrayList<>();
            for (KakaoTokenStore.StoredToken token : tokens) {
                if (token.refreshExpiresAt().isBefore(Instant.now())) {
                    tokenStore.delete(token.userId());
                    continue;
                }
                connections.add(new ConnectionSummary(
                        token.userId(), maskedAccountLabel(token.userId()), token.refreshExpiresAt()
                ));
            }
            if (connections.isEmpty()) {
                return new ConfigStatus(false, true, "카카오 계정을 한 번 연결해 주세요.", List.of());
            }
            return new ConfigStatus(
                    true,
                    true,
                    "카카오 " + connections.size() + "개 계정 연결됨 · 토큰 자동 갱신",
                    connections
            );
        } catch (IllegalStateException error) {
            return new ConfigStatus(false, true, error.getMessage(), List.of());
        }
    }

    public void disconnect(String userId) {
        tokenStore.delete(userId);
    }

    public void disconnectAll() {
        tokenStore.deleteAll();
    }

    public String frontendReturnUrl() {
        return frontendReturnUrl;
    }

    private MultiValueMap<String, String> tokenRequest(String code) {
        MultiValueMap<String, String> form = baseTokenForm("authorization_code");
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        return form;
    }

    private MultiValueMap<String, String> refreshRequest(String refreshToken) {
        MultiValueMap<String, String> form = baseTokenForm("refresh_token");
        form.add("refresh_token", refreshToken);
        return form;
    }

    private MultiValueMap<String, String> baseTokenForm(String grantType) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        return form;
    }

    private Map<?, ?> requestToken(MultiValueMap<String, String> form) {
        try {
            Map<?, ?> response = authClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (response == null) throw new IllegalStateException("카카오 토큰 응답이 비어 있습니다.");
            return response;
        } catch (RestClientResponseException error) {
            throw new IllegalStateException("카카오 토큰 요청 실패: HTTP " + error.getStatusCode().value(), error);
        }
    }

    private String tokenOwnerId(String accessToken) {
        try {
            Map<?, ?> owner = apiClient.get()
                    .uri("/v1/user/access_token_info")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            String id = owner == null ? "" : String.valueOf(owner.get("id"));
            if (id.isBlank() || "null".equals(id)) {
                throw new IllegalStateException("카카오 사용자 정보를 확인할 수 없습니다.");
            }
            return id;
        } catch (RestClientResponseException error) {
            throw new IllegalStateException("카카오 사용자 확인 실패: HTTP " + error.getStatusCode().value(), error);
        }
    }

    private boolean oauthConfigured() {
        return clientId != null && !clientId.isBlank()
                && redirectUri != null && !redirectUri.isBlank()
                && tokenStore.encryptionConfigured();
    }

    private String maskedAccountLabel(String userId) {
        int visibleFrom = Math.max(0, userId.length() - 6);
        return "카카오 계정 · " + userId.substring(visibleFrom);
    }

    private void requireOAuthConfiguration() {
        if (!oauthConfigured()) {
            throw new IllegalStateException("KAKAO_CLIENT_ID, KAKAO_REDIRECT_URI, KAKAO_TOKEN_ENCRYPTION_KEY 설정이 필요합니다.");
        }
    }

    private String requiredString(Map<?, ?> values, String key) {
        return optionalString(values, key)
                .orElseThrow(() -> new IllegalStateException("카카오 응답에 " + key + " 값이 없습니다."));
    }

    private Optional<String> optionalString(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) return Optional.empty();
        return Optional.of(String.valueOf(value));
    }

    private long number(Map<?, ?> values, String key, long fallback) {
        return optionalNumber(values, key).orElse(fallback);
    }

    private Optional<Long> optionalNumber(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) return Optional.of(number.longValue());
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(String.valueOf(value)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public record AuthorizationStart(String authorizationUrl) {
    }

    public record OAuthResult(String userId, String returnUrl) {
    }

    public record ConfigStatus(
            boolean configured,
            boolean oauthReady,
            String message,
            List<ConnectionSummary> connections
    ) {
    }

    public record ConnectionSummary(String userId, String label, Instant refreshExpiresAt) {
    }
}
