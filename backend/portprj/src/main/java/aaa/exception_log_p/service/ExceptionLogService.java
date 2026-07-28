package aaa.exception_log_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.model.ExceptionLogMapper;
import aaa.exception_log_p.model.ExceptionTimelineEventDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class  ExceptionLogService {

    @Resource
    ExceptionLogMapper mapper;

    public List<ExceptionLogDTO> list() {
        List<ExceptionLogDTO> exceptionLogs = mapper.list();
        for (ExceptionLogDTO exceptionLog : exceptionLogs) {
            exceptionLog.setTimeline(buildTimeline(exceptionLog));
        }
        return exceptionLogs;
    }

    public int insert(ExceptionLogDTO dto) {
        if (dto.getOccurredTime() == null) {
            dto.setOccurredTime(LocalDateTime.now());
        }
        if (dto.getProcessStatus() == null || dto.getProcessStatus().isBlank()) {
            dto.setProcessStatus("UNPROCESSED");
        }
        return mapper.insert(dto);
    }

    public int updateProcess(Long exceptionLogId, ExceptionLogDTO dto) {
        if (dto == null) {
            dto = new ExceptionLogDTO();
        }
        dto.setExceptionLogId(exceptionLogId);
        if (dto.getProcessedTime() == null) {
            dto.setProcessedTime(LocalDateTime.now());
        }
        if (dto.getProcessStatus() == null || dto.getProcessStatus().isBlank()) {
            dto.setProcessStatus("PROCESSED");
        }
        return mapper.updateProcess(dto);
    }

    public int updateProcessByGateLogId(Long gateLogId, String managerAction) {
        if (gateLogId == null) {
            return 0;
        }
        if (managerAction == null || managerAction.isBlank()) {
            managerAction = "수동 보정 완료";
        }
        return mapper.updateProcessByGateLogId(gateLogId, managerAction, LocalDateTime.now());
    }

    private List<ExceptionTimelineEventDTO> buildTimeline(ExceptionLogDTO dto) {
        List<ExceptionTimelineEventDTO> timeline = new ArrayList<>();

        if (dto.getEntryTime() != null || dto.getExitTime() != null) {
            String inOutType = dto.getInOutType();
            LocalDateTime gateTime = "OUT".equalsIgnoreCase(inOutType) ? dto.getExitTime() : dto.getEntryTime();
            if (gateTime == null) {
                gateTime = dto.getEntryTime() == null ? dto.getExitTime() : dto.getEntryTime();
            }
            timeline.add(event(
                    "GATE",
                    "OUT".equalsIgnoreCase(inOutType) ? "Gate out" : "Gate in",
                    gateTime,
                    dto.getGateName()
            ));
        }

        timeline.add(event(
                "EXCEPTION",
                "Exception occurred",
                dto.getOccurredTime(),
                dto.getExceptionMessage()
        ));

        if (dto.getProcessedTime() != null) {
            timeline.add(event(
                    "PROCESSED",
                    "Exception processed",
                    dto.getProcessedTime(),
                    dto.getManagerAction()
            ));
        }

        timeline.sort(Comparator.comparing(
                ExceptionTimelineEventDTO::getEventTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return timeline;
    }

    private ExceptionTimelineEventDTO event(
            String eventType,
            String eventLabel,
            LocalDateTime eventTime,
            String description
    ) {
        ExceptionTimelineEventDTO event = new ExceptionTimelineEventDTO();
        event.setEventType(eventType);
        event.setEventLabel(eventLabel);
        event.setEventTime(eventTime);
        event.setDescription(description);
        return event;
    }
}
