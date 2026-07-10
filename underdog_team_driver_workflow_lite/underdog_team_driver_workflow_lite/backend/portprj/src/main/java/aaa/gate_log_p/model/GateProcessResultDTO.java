package aaa.gate_log_p.model;

import lombok.Data;

@Data
public class GateProcessResultDTO {
    private Boolean success;
    private Long gateLogId;
    private Long workOrderId;
    private String workStatus;
    private String processResult;
    private String exceptionType;
    private String message;
}
