package aaa.predictive_maintenance_p.model;

import java.time.LocalDateTime;

public record PredictiveImportResult(
        String sourceFile,
        int equipmentCount,
        int sensorRowsUpserted,
        int eventRowsUpserted,
        LocalDateTime firstCollectedAt,
        LocalDateTime lastCollectedAt
) {
}
