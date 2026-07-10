package aaa.yard_sector_p.model;

import lombok.Data;

@Data
public class YardSectorDTO {
    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String sectorStatus;
    private Integer waitingVehicleCount;
    private String guideMessage;
    private String altWaitingArea;
}
