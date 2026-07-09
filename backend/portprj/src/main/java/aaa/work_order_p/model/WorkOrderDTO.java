package aaa.work_order_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderDTO {
    private Long workOrderId;
    private String workType;
    private Long vehicleId;
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private Long driverId;
    private Long containerId;
    private LocalDateTime reservedTime;
    private String workStatus;
    private Boolean isApproved;
}
