package aaa.work_order_p.model;

import lombok.Data;

@Data
public class TrailerWorkInfoDTO {

    private Long workOrderId;
    private String workType;

    private Long vehicleId;
    private Long containerId;
    private String containerNumber;

    private Long sectorId;
    private String sectorName;
    private String guideMessage;
}
