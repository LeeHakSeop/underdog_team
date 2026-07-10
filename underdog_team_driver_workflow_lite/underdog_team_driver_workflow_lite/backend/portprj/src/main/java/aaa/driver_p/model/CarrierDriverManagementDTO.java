package aaa.driver_p.model;

import lombok.Data;

@Data
public class CarrierDriverManagementDTO {

    // 기사/계정 정보
    private Long driverId;
    private Long userId;
    private String loginId;
    private String userStatus;
    private String driverName;
    private String driverContact;
    private Boolean isRegistered;
    private Boolean canEnter;

    // 소속 운송사 정보
    private Long carrierId;
    private String carrierName;

    // 기사 회원가입 시 등록한 트랙터 정보
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private String tonnage;
    private Boolean vehicleRegistered;
    private String vehicleStatus;
    private String tractorNo;
    private String chassisNo;
}
