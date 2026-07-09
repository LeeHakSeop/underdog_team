package aaa.vehicle_p.model;

import lombok.Data;

@Data
public class VehicleDTO {

    private Long vehicleId;

    // 차량
    private String plateNumber;
    private String vehicleType;
    private String tonnage;

    // 승인
    private Boolean isRegistered;
    private String vehicleStatus;

    // 차량 정보
    private String tractorNo;
    private String chassisNo;

    // 관계
    private Long driverId;     // ★ 추가
    private Long carrierId;
    private Long userId;
}