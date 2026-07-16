package aaa.driver_p.model;

import lombok.Data;

@Data
public class DriverDTO {

    private Long driverId;

    private String driverName;

    private String driverContact;

    /**
     * 운송사 승인 여부
     */
    private Boolean isRegistered;

    /**
     * 소속 운송사
     */
    private Long carrierId;

    /**
     * 관리자 최종 승인 후 출입 가능 여부
     */
    private Boolean canEnter;

    /**
     * users.user_id
     */
    private Long userId;

    /**
     * users.status
     */
    private String userStatus;
}
