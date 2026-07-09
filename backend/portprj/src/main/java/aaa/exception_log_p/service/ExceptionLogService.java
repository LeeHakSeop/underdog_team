package aaa.exception_log_p.service;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.model.ExceptionLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExceptionLogService {

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
        dto.setExceptionLogId(exceptionLogId);
        if (dto.getProcessedTime() == null) {
            dto.setProcessedTime(LocalDateTime.now());
        }
        if (dto.getProcessStatus() == null || dto.getProcessStatus().isBlank()) {
            dto.setProcessStatus("PROCESSED");
        }
        return mapper.updateProcess(dto);
    }
}
