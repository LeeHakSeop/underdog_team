package aaa.predictive_maintenance_p.controller;

import aaa.predictive_maintenance_p.model.DemoKakaoNotificationRequest;
import aaa.predictive_maintenance_p.service.KakaoMessageService;
import aaa.predictive_maintenance_p.service.KakaoOAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/predictive-maintenance/demo/notifications")
public class PredictiveMaintenanceNotificationController {

    private static final Logger log = LoggerFactory.getLogger(PredictiveMaintenanceNotificationController.class);
    private static final Set<String> SUPPORTED_EVENTS = Set.of("FAILURE_EXPECTED", "FAILURE");
    private final KakaoMessageService kakaoMessageService;
    private final KakaoOAuthService kakaoOAuthService;

    public PredictiveMaintenanceNotificationController(
            KakaoMessageService kakaoMessageService,
            KakaoOAuthService kakaoOAuthService
    ) {
        this.kakaoMessageService = kakaoMessageService;
        this.kakaoOAuthService = kakaoOAuthService;
    }

    @PostMapping("/kakao")
    public ResponseEntity<Map<String, String>> sendKakao(
            @Valid @RequestBody DemoKakaoNotificationRequest request
    ) {
        if (!SUPPORTED_EVENTS.contains(request.eventType())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "message", "고장 예상과 실제 고장 알림만 발송할 수 있습니다."
            ));
        }

        KakaoMessageService.SendResult result = kakaoMessageService.sendDemoAlert(request);
        HttpStatus status = switch (result.status()) {
            case "SENT", "PARTIAL", "DRY_RUN" -> HttpStatus.OK;
            case "NOT_CONFIGURED" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "BLOCKED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(Map.of(
                "status", result.status(),
                "message", result.message()
        ));
    }

    @GetMapping("/kakao/config")
    public KakaoOAuthService.ConfigStatus kakaoConfigStatus() {
        return kakaoOAuthService.status();
    }

    @GetMapping("/kakao/oauth/authorize")
    public KakaoOAuthService.AuthorizationStart authorizeKakao() {
        return kakaoOAuthService.startAuthorization();
    }

    @GetMapping("/kakao/oauth/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        String result = "connected";
        if (error != null && !error.isBlank()) {
            result = "denied";
        } else {
            try {
                kakaoOAuthService.completeAuthorization(code, state);
            } catch (RuntimeException callbackError) {
                log.error("Kakao OAuth callback failed: {}", callbackError.getMessage(), callbackError);
                result = "failed";
            }
        }

        URI location = UriComponentsBuilder
                .fromUriString(kakaoOAuthService.frontendReturnUrl())
                .queryParam("kakao", result)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }

    @DeleteMapping("/kakao/config")
    public Map<String, String> clearKakaoConfig() {
        kakaoOAuthService.disconnectAll();
        return Map.of(
                "status", "CLEARED",
                "message", "저장된 모든 카카오 연결 정보를 삭제했습니다."
        );
    }

    @DeleteMapping("/kakao/config/{userId}")
    public Map<String, String> clearKakaoConfig(@PathVariable String userId) {
        kakaoOAuthService.disconnect(userId);
        return Map.of(
                "status", "CLEARED",
                "message", "선택한 카카오 계정 연결을 해제했습니다."
        );
    }
}
