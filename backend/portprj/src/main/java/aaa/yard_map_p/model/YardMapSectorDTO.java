package aaa.yard_map_p.model;

import lombok.Data;

@Data
public class YardMapSectorDTO {
    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String sectorStatus;
    private String environmentType;
    private Integer waitingVehicleCount;
    private String guideMessage;
    private String altWaitingArea;
    private Integer capacity;
    private Long containerCount;
    private Double usageRate;
    private Long workOrderCount;
    private String statusLevel;
}
