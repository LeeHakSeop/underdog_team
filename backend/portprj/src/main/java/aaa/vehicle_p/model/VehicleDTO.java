package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class VehicleDTO {
    Long vehicleId;
    String plateNumber;
    String vehicleType;
    String tonnage;
    Boolean isRegistered;
    String vehicleStatus;
    String tractorNo;
    String chassisNo;
    Long carrierId;

}
