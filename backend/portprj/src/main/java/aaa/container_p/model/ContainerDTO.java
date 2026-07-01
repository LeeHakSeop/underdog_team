package aaa.container_p.model;

import lombok.Data;

@Data
public class ContainerDTO {
    private Long containerId;
    private String containerNumber;
    private String containerSize;
    private String containerLocation;
    private Long sectorId;
    private String block;
    private String bay;
    private String rowNo;
    private Boolean canExit;
    private String sealNumber;
    private String shippingLine;
}
