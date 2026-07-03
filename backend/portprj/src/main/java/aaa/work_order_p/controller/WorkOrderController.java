package aaa.work_order_p.controller;

import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
public class WorkOrderController {

    @Resource
    WorkOrderService service;

    @GetMapping
    public List<WorkOrderDTO> list() {
        return service.list();
    }

    @PostMapping
    public WorkOrderDTO reg(@RequestBody WorkOrderDTO dto) {
        service.insert(dto);
        return dto;
    }
}
