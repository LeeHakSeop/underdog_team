package aaa.predictive_maintenance_p.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DemoKakaoNotificationRequest(
        @NotBlank String eventKey,
        @NotBlank String equipmentId,
        @NotBlank String eventType,
        @NotNull Long occurredAt,
        Long requestedAt
) {
}
