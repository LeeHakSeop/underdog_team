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
import java.util.Map;

@Service
public class KakaoMessageService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

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

    private volatile String runtimeAccessToken = "";
    private volatile String runtimeUserId = "";

    public KakaoMessageService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://kapi.kakao.com").build();
        this.objectMapper = objectMapper;
    }

    public SendResult sendDemoAlert(DemoKakaoNotificationRequest request) {
        String effectiveAccessToken = runtimeAccessToken.isBlank() ? accessToken : runtimeAccessToken;
        String effectiveUserId = runtimeUserId.isBlank() ? allowedUserId : runtimeUserId;
        boolean runtimeConfigured = !runtimeAccessToken.isBlank();

        if (!enabled && !runtimeConfigured) {
            return new SendResult("NOT_CONFIGURED", "카카오 발송 기능이 비활성화되어 있습니다.");
        }
        if (dryRun && !runtimeConfigured) {
            return new SendResult("DRY_RUN", "모의 발송 상태이므로 실제 카카오톡은 전송하지 않았습니다.");
        }
        if (effectiveAccessToken.isBlank() || effectiveUserId.isBlank()) {
            return new SendResult("NOT_CONFIGURED", "카카오 토큰 또는 허용된 본인 사용자 ID가 없습니다.");
        }

        String title = switch (request.eventType()) {
            case "FAILURE_EXPECTED" -> "[안테나 고장 예상]";
            case "FAILURE" -> "[안테나 실제 고장]";
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
            form.add("template_object", objectMapper.writeValueAsString(template));

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
        } catch (JsonProcessingException error) {
            return new SendResult("FAILED", "카카오 메시지 생성에 실패했습니다.");
        } catch (RestClientResponseException error) {
            return new SendResult("FAILED", "카카오 발송 실패: HTTP " + error.getStatusCode().value());
        }
    }

    public SendResult configureRuntime(String token) {
        String candidate = token == null ? "" : token.trim();
        if (candidate.isBlank()) {
            return new SendResult("REJECTED", "액세스 토큰을 입력해야 합니다.");
        }

        try {
            Map<?, ?> tokenOwner = restClient.get()
                    .uri("/v1/user/access_token_info")
                    .header("Authorization", "Bearer " + candidate)
                    .retrieve()
                    .body(Map.class);
            String ownerId = tokenOwner == null ? "" : String.valueOf(tokenOwner.get("id"));
            if (ownerId.isBlank() || "null".equals(ownerId)) {
                return new SendResult("FAILED", "토큰 소유자 정보를 확인할 수 없습니다.");
            }
            runtimeAccessToken = candidate;
            runtimeUserId = ownerId;
            return new SendResult("READY", "현재 백엔드 실행에만 본인 카카오 계정이 연결되었습니다.");
        } catch (RestClientResponseException error) {
            return new SendResult("FAILED", "카카오 토큰 확인 실패: HTTP " + error.getStatusCode().value());
        }
    }

    public ConfigStatus configStatus() {
        if (!runtimeAccessToken.isBlank()) {
            return new ConfigStatus(true, "MEMORY", "현재 실행에만 연결됨 · 백엔드 종료 시 삭제");
        }
        if (enabled && !accessToken.isBlank() && !allowedUserId.isBlank()) {
            return new ConfigStatus(true, "ENVIRONMENT", "환경변수로 연결됨");
        }
        return new ConfigStatus(false, "NONE", "카카오 계정이 연결되지 않았습니다.");
    }

    public void clearRuntime() {
        runtimeAccessToken = "";
        runtimeUserId = "";
    }

    public record SendResult(String status, String message) {
    }

    public record ConfigStatus(boolean configured, String source, String message) {
    }
}
