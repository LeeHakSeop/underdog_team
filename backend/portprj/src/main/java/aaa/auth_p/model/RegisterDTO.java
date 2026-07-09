package aaa.auth_p.model;

import lombok.Data;

@Data
public class RegisterDTO {

    // users 테이블
    private Long userId;
    private String loginId;
    private String password;
    private String userName;
    private String roleCode;
    private String status;

    // carrier 테이블
    private String carrierName;
    private String carrierContact;
    private String managerName;
    private String carrierStatus;

    // driver 테이블
    private String driverName;
    private String driverContact;
    private Long carrierId;
    private Boolean isRegistered;
    private Boolean canEnter;

    // vehicle 테이블
    private String plateNumber;
    private String vehicleType;
    private String tonnage;
    private Boolean vehicleRegistered;
    private String vehicleStatus;
    private String tractorNo;
    private String chassisNo;
}