package aaa.exception_log_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionTimelineEventDTO {
    private String eventType;
    private String eventLabel;
    private LocalDateTime eventTime;
    private String description;
}
