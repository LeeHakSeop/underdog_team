package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class VehicleDTO {

    private Long vehicleId;

    // 차량 기본 정보
    private String plateNumber;
    private String vehicleType;
    private String tonnage;

    // 승인 상태
    private Boolean isRegistered;
    private String vehicleStatus;

    // 세부 차량 정보
    private String tractorNo;
    private String chassisNo;

    // 연관 관계
    private Long driverId;
    private Long carrierId;
    private Long userId;
}