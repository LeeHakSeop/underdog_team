package aaa.gate_log_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GateLogDTO {
    private Long gateLogId;
    private Long vehicleId;
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private String gateNumber;
    private String gateName;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String inOutType;
    private String processResult;
    private Boolean managerCheck;
}
