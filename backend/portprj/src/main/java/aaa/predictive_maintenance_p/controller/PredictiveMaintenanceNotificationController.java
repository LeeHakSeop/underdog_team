package aaa.predictive_maintenance_p.controller;

import aaa.predictive_maintenance_p.model.DemoKakaoNotificationRequest;
import aaa.predictive_maintenance_p.service.KakaoMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/predictive-maintenance/demo/notifications")
public class PredictiveMaintenanceNotificationController {

    private static final Set<String> SUPPORTED_EVENTS = Set.of("FAILURE_EXPECTED", "FAILURE");
    private final KakaoMessageService kakaoMessageService;

    public PredictiveMaintenanceNotificationController(KakaoMessageService kakaoMessageService) {
        this.kakaoMessageService = kakaoMessageService;
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
            case "SENT", "DRY_RUN" -> HttpStatus.OK;
            case "NOT_CONFIGURED" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "BLOCKED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(Map.of(
                "status", result.status(),
                "message", result.message()
        ));
    }
}
