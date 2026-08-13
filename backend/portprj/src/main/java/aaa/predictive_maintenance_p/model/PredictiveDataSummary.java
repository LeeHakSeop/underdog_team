package aaa.predictive_maintenance_p.model;

import java.time.LocalDateTime;

public record PredictiveDataSummary(
        long equipmentCount,
        long sensorDataCount,
        long eventCount,
        LocalDateTime firstCollectedAt,
        LocalDateTime lastCollectedAt
) {
}
