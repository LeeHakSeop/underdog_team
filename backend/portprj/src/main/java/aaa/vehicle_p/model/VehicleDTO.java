package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class VehicleDTO {
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private String tonnage;
    private Boolean isRegistered;
    private String vehicleStatus;
    private String tractorNo;
    private String chassisNo;
    private Long carrierId;
}
