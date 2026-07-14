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
}
