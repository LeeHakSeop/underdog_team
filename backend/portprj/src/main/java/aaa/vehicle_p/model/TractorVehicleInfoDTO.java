package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class TractorVehicleInfoDTO {
    private Long vehicleId;
    private String plateNumber;
    private Boolean registeredVehicle;
    private String registeredText;
    private String carrierName;
    private String driverName;
    private String vehicleStatus;
    private String tractorNo;
}
