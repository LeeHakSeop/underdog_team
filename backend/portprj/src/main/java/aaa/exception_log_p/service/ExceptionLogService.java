package aaa.exception_log_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.model.ExceptionLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class  ExceptionLogService {

    @Resource
    ExceptionLogMapper mapper;

    public List<ExceptionLogDTO> list() {
        return mapper.list();
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
}
