package aaa.predictive_maintenance_p.service;

import aaa.predictive_maintenance_p.model.DemoKakaoNotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class KakaoMessageService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KakaoOAuthService kakaoOAuthService;

    @Value("${kakao.message.enabled:false}")
    private boolean enabled;

    @Value("${kakao.message.access-token:}")
    private String accessToken;

    @Value("${kakao.message.dry-run:true}")
    private boolean dryRun;

    @Value("${kakao.message.allowed-user-id:}")
    private String allowedUserId;

    @Value("${kakao.message.dashboard-url:http://localhost:5173}")
    private String dashboardUrl;

    public KakaoMessageService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            KakaoOAuthService kakaoOAuthService
    ) {
        this.restClient = restClientBuilder.baseUrl("https://kapi.kakao.com").build();
        this.objectMapper = objectMapper;
        this.kakaoOAuthService = kakaoOAuthService;
    }

    public SendResult sendDemoAlert(DemoKakaoNotificationRequest request) {
        List<KakaoTokenStore.StoredToken> oauthTokens;
        int connectedAccountCount;
        try {
            connectedAccountCount = kakaoOAuthService.connectionCount();
            oauthTokens = kakaoOAuthService.validTokens();
        } catch (IllegalStateException error) {
            return new SendResult("FAILED", error.getMessage());
        }

        boolean oauthConfigured = connectedAccountCount > 0;

        if (!enabled && !oauthConfigured) {
            return new SendResult("NOT_CONFIGURED", "카카오 발송 기능이 비활성화되어 있습니다.");
        }
        if (dryRun && !oauthConfigured) {
            return new SendResult("DRY_RUN", "모의 발송 상태이므로 실제 카카오톡은 전송하지 않았습니다.");
        }
        if (!oauthConfigured && (accessToken.isBlank() || allowedUserId.isBlank())) {
            return new SendResult("NOT_CONFIGURED", "카카오 토큰 또는 허용된 본인 사용자 ID가 없습니다.");
        }

        String title = switch (request.eventType()) {
            case "FAILURE_EXPECTED" -> "[항만 운영장비 고장 예상]";
            case "FAILURE" -> "[항만 운영장비 실제 고장]";
            default -> throw new IllegalArgumentException("지원하지 않는 카카오 알림 유형입니다.");
        };
        String occurredAt = DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(request.occurredAt()));
        String message = String.join("\n",
                title,
                "설비: " + request.equipmentId(),
                "발생 시각: " + occurredAt,
                request.eventType().equals("FAILURE_EXPECTED")
                        ? "고장 전조가 확인되었습니다. 점검이 필요합니다."
                        : "고장이 확인되었습니다. 즉시 확인이 필요합니다."
        );

        Map<String, Object> template = Map.of(
                "object_type", "text",
                "text", message,
                "link", Map.of(
                        "web_url", dashboardUrl,
                        "mobile_web_url", dashboardUrl
                ),
                "button_title", "예지보전 화면 확인"
        );

        final String templateJson;
        try {
            templateJson = objectMapper.writeValueAsString(template);
        } catch (JsonProcessingException error) {
            return new SendResult("FAILED", "카카오 메시지 생성에 실패했습니다.");
        }

        if (!oauthConfigured) {
            return sendToAccount(accessToken, allowedUserId, templateJson);
        }

        int sent = 0;
        for (KakaoTokenStore.StoredToken token : oauthTokens) {
            SendResult result = sendToAccount(token.accessToken(), token.userId(), templateJson);
            if ("SENT".equals(result.status())) sent++;
        }

        if (sent == connectedAccountCount) {
            return new SendResult("SENT", "카카오톡 " + sent + "개 계정에 발송했습니다.");
        }
        if (sent > 0) {
            return new SendResult(
                    "PARTIAL",
                    "카카오톡 " + sent + "/" + connectedAccountCount + "개 계정에 발송했습니다."
            );
        }
        return new SendResult("FAILED", "연결된 카카오 계정에 발송하지 못했습니다.");
    }

    private SendResult sendToAccount(String effectiveAccessToken, String effectiveUserId, String templateJson) {
        try {
            Map<?, ?> tokenOwner = restClient.get()
                    .uri("/v1/user/access_token_info")
                    .header("Authorization", "Bearer " + effectiveAccessToken)
                    .retrieve()
                    .body(Map.class);
            String tokenOwnerId = tokenOwner == null ? "" : String.valueOf(tokenOwner.get("id"));
            if (!effectiveUserId.equals(tokenOwnerId)) {
                return new SendResult("BLOCKED", "액세스 토큰의 계정이 허용된 본인 계정과 일치하지 않습니다.");
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("template_object", templateJson);

            Map<?, ?> response = restClient.post()
                    .uri("/v2/api/talk/memo/default/send")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Bearer " + effectiveAccessToken)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response != null && Integer.valueOf(0).equals(response.get("result_code"))) {
                return new SendResult("SENT", "카카오톡 나에게 보내기가 완료되었습니다.");
            }
            return new SendResult("FAILED", "카카오 응답을 확인할 수 없습니다.");
        } catch (RestClientResponseException error) {
            return new SendResult("FAILED", "카카오 발송 실패: HTTP " + error.getStatusCode().value());
        }
    }

    public record SendResult(String status, String message) {
    }
}
