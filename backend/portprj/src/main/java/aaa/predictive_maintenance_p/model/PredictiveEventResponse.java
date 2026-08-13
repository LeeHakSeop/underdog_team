package aaa.predictive_maintenance_p.model;

import java.time.LocalDateTime;
import java.util.List;

public record PredictiveEventResponse(
        Long eventId,
        String equipmentId,
        String eventType,
        LocalDateTime occurredAt,
        Integer anomalyCount,
        Double currentFaultProbability,
        List<String> abnormalSensors,
        String eventMessage,
        String sourceType,
        String notificationStatus,
        LocalDateTime notificationSentAt
) {
}
