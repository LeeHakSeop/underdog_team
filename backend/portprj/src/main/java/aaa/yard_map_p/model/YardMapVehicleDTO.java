package aaa.yard_map_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YardMapVehicleDTO {
    private Long workOrderId;
    private String workType;
    private String workStatus;
    private Long vehicleId;
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private String tractorPlateNumber;
    private String trailerPlateNumber;
    private String tractorVehicleStatus;
    private String trailerVehicleStatus;
    private Long containerId;
    private String containerNumber;
    private String containerSize;
    private String containerLocation;
    private Boolean canExit;
    private String driverName;
    private String carrierName;
    private Long sectorId;
    private String sectorName;
    private String blockName;
    private String originLocation;
    private Long startSectorId;
    private String startSectorName;
    private Long destinationSectorId;
    private String destinationSectorName;
    private String routeSummary;
    private LocalDateTime reservedTime;
}
