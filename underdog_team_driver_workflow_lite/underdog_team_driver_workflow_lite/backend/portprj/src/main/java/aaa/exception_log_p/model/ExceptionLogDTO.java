package aaa.exception_log_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionLogDTO {
    private Long exceptionLogId;
    private Long gateLogId;
    private Long vehicleId;
    private String plateNumber;
    private String exceptionType;
    private String exceptionMessage;
    private LocalDateTime occurredTime;
    private String processStatus;
    private String managerAction;
    private LocalDateTime processedTime;
}
