package aaa.work_order_p.model;

import lombok.Data;

@Data
public class TrailerWorkInfoDTO {

    private Long workOrderId;
    private String workType;

    private Long vehicleId;
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private Long driverId;
    private Long containerId;
    private String workStatus;
    private Boolean isApproved;
    private String containerNumber;
    private String containerLocation;
    private String yardLocation;
    private String block;
    private String bay;
    private String rowNo;

    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String sectorStatus;
    private String guideMessage;
    private String altWaitingArea;
    private String workGuideMessage;
}
