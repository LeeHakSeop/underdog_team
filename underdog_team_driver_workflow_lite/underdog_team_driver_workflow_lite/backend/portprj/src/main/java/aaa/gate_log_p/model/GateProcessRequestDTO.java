package aaa.gate_log_p.model;

import lombok.Data;

@Data
public class GateProcessRequestDTO {
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private Long workOrderId;
    private Long containerId;
    private Long sectorId;
    private String gateNumber;
    private String gateName;
    private String inOutType;
}
