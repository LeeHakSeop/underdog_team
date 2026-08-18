package aaa.predictive_maintenance_p.model;

import java.time.LocalDateTime;
import java.util.List;

public record PredictiveSensorDataResponse(
        Long sensorDataId,
        String equipmentId,
        LocalDateTime collectedAt,
        Double trafficLoad,
        Double temperatureC,
        Double voltageV,
        Double signalStrengthDbm,
        Double successRate,
        Double responseTimeMs,
        Integer retryCount,
        Integer disconnectCount,
        Double packetLossRate,
        Integer errorCount,
        Integer daysSinceMaintenance,
        Double currentFaultProbability,
        Double progressionProbability,
        String progressionModel,
        int anomalyCount,
        List<String> abnormalSensors,
        boolean currentFailure,
        String operationalState,
        boolean precursorEntryCondition,
        boolean stateChanged,
        String operationalStateKo,
        int stateLevel,
        boolean needsAttention,
        int failureEvent,
        int maintenanceEvent
) {
}
