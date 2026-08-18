package aaa.yard_congestion_p.model;

import lombok.Data;

@Data
public class YardCongestionSectorDTO {
    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String sectorStatus;
    private Integer capacity;
    private Long containerCount;
    private Double usageRate;
    private Integer waitingVehicleCount;
    private Long workOrderCount;
    private String statusLevel;
    private String statusLabel;
    private String congestionReason;
    private String guideMessage;
    private String altWaitingArea;
}
