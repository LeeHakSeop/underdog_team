package aaa.predictive_maintenance_p.model;

public record PredictiveEquipmentResponse(
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String equipmentType,
        String locationCode,
        String operationStatus,
        boolean enabled
) {
}
