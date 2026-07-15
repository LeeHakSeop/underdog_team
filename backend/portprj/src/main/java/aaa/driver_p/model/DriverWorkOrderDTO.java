package aaa.driver_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverWorkOrderDTO {
    private Long workOrderId;
    private String workType;
    private LocalDateTime reservedTime;
    private String workStatus;
    private Boolean isApproved;
    private Long driverId;
    private String driverName;
    private String driverContact;
    private Boolean canEnter;
    private Long carrierId;
    private String carrierName;
    private String carrierContact;
    private Long vehicleId;
    private Long tractorVehicleId;
    private Long trailerVehicleId;
    private String plateNumber;
    private String trailerPlateNumber;
    private String vehicleType;
    private String vehicleStatus;
    private Long containerId;
    private String containerNumber;
    private String containerSize;
    private String containerLocation;
    private String block;
    private String bay;
    private String rowNo;
    private Long sectorId;
    private String sectorName;
    private String sectorStatus;
    private String guideMessage;
    private Boolean canExit;
    private String altWaitingArea;
}
