package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class VehicleDTO {
    int vehicleId;
    String plateNumber;
    String vehicleType;
    String tonnage;
    boolean isRegistered;
    String vehicleStatus;
    String tractorNo;
    String chassisNo;
    int carrierId;

}
