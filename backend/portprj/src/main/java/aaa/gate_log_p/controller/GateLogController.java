package aaa.gate_log_p.controller;

import aaa.gate_log_p.model.GateProcessRequestDTO;
import aaa.gate_log_p.model.GateProcessResultDTO;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.gate_log_p.service.GateLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gate-log")
public class GateLogController {

    @Resource
    GateLogService service;

    @GetMapping
    public List<GateLogDTO> list() {
        return service.list();
    }

    @PostMapping("/process")
    public GateProcessResultDTO process(@RequestBody GateProcessRequestDTO dto) {
        return service.process(dto);
    }
}
