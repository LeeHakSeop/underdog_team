package aaa.gate_log_p.service;

import aaa.gate_log_p.model.GateLogDTO;
import aaa.gate_log_p.model.GateLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GateLogService {

    @Resource
    GateLogMapper mapper;

    public List<GateLogDTO> list() {
        return mapper.list();
    }
}
