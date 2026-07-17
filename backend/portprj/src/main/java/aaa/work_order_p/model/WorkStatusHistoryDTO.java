package aaa.work_order_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkStatusHistoryDTO {
    private Long historyId;
    private Long workOrderId;
    private String prevStatus;
    private String newStatus;
    private LocalDateTime changedTime;
    private String changedBy;
    private String reason;
    private String remark;

    // 작업 이력 조회 화면에서 함께 표시할 작업·기사 정보입니다.
    private String workType;
    private LocalDateTime reservedTime;
    private Long driverId;
    private String driverName;
    private String plateNumber;
    private String containerNumber;
}
