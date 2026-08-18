package aaa.dashboard_p.model;

import lombok.Data;

@Data
public class DashboardSectorDTO {
    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String sectorStatus;
    private Integer waitingVehicleCount;
    private String guideMessage;
    private String altWaitingArea;
    private Integer capacity;
    private Integer containerCount;
    private Double usageRate;
    private Integer workOrderCount;
    private String statusLevel;
}
