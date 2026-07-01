package aaa.driver_p.model;

import lombok.Data;

@Data
public class DriverDTO {
    private Long driverId;
    private String driverName;
    private String driverContact;
    private Boolean isRegistered;
    private Long carrierId;
    private Boolean canEnter;
}
