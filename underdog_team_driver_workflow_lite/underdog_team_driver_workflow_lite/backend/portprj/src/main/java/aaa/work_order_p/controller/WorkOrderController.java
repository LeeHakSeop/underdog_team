package aaa.work_order_p.controller;

import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-order")
public class WorkOrderController {
    @Resource WorkOrderService service;

    @GetMapping public List<WorkOrderDTO> list() { return service.list(); }

    @GetMapping("/carrier/user/{userId}")
    public List<WorkOrderDTO> carrierWorkOrders(@PathVariable Long userId) {
        return service.carrierWorkOrders(userId);
    }

    @PostMapping
    public WorkOrderDTO create(@RequestBody WorkOrderDTO dto) {
        service.insert(dto);
        return dto;
    }

    @PatchMapping("/{workOrderId}/approve")
    public WorkOrderDTO approve(@PathVariable Long workOrderId) {
        return service.approve(workOrderId);
    }

    @PatchMapping("/{workOrderId}/status")
    public WorkOrderDTO updateStatus(@PathVariable Long workOrderId, @RequestBody Map<String, String> body) {
        String status = body.get("workStatus");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("workStatus가 필요합니다.");
        return service.updateStatus(workOrderId, status);
    }

    @GetMapping("/trailer-info/{vehicleId}")
    public TrailerWorkInfoDTO trailerInfo(@PathVariable Long vehicleId) {
        return service.findTrailerWorkInfo(vehicleId);
    }
}
