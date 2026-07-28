package aaa.work_order_p.model;

import lombok.Data;

@Data
public class WorkOrderProcessResultDTO {
    private boolean success;
    private Long workOrderId;
    private String workStatus;
    private String processResult;
    private String exceptionType;
    private String message;
}
