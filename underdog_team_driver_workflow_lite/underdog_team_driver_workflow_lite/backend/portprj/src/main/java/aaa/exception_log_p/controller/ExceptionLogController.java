package aaa.exception_log_p.controller;

import aaa.exception_log_p.model.ExceptionLogDTO;
import aaa.exception_log_p.service.ExceptionLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exception-log")
public class ExceptionLogController {

    @Resource
    ExceptionLogService service;

    @GetMapping
    public List<ExceptionLogDTO> list() {
        return service.list();
    }

    @PutMapping("/{exceptionLogId}/process")
    public int process(@PathVariable Long exceptionLogId, @RequestBody ExceptionLogDTO dto) {
        return service.updateProcess(exceptionLogId, dto);
    }
}
