package aaa.work_order_p.controller;

import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderProcessResultDTO;
import aaa.work_order_p.model.WorkStatusHistoryDTO;
import aaa.work_order_p.service.WorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-order")
public class WorkOrderController {

    @Resource
    WorkOrderService service;

    @GetMapping
    public List<WorkOrderDTO> list() {
        return service.list();
    }

    @GetMapping("/history")
    public List<WorkStatusHistoryDTO> history() {
        return service.history();
    }

    @GetMapping("/history/driver/{userId}")
    public List<WorkStatusHistoryDTO> historyByDriverUserId(@PathVariable Long userId) {
        return service.historyByDriverUserId(userId);
    }

    @GetMapping("/{workOrderId}")
    public WorkOrderDTO detail(@PathVariable Long workOrderId) {
        return service.detail(workOrderId);
    }

    @PostMapping
    public WorkOrderDTO create(@RequestBody WorkOrderDTO dto) {
        service.insert(dto);
        return dto;
    }

    @PutMapping("/{workOrderId}")
    public WorkOrderDTO update(
            @PathVariable Long workOrderId,
            @RequestBody WorkOrderDTO dto
    ) {
        dto.setWorkOrderId(workOrderId);
        return service.update(dto);
    }

    @DeleteMapping("/{workOrderId}")
    public WorkOrderDTO delete(@PathVariable Long workOrderId) {
        return service.cancel(workOrderId);
    }

    @GetMapping("/trailer-info/{vehicleId}")
    public TrailerWorkInfoDTO trailerInfo(@PathVariable Long vehicleId) {
        return service.findTrailerWorkInfo(vehicleId);
    }

    @PatchMapping("/{workOrderId}/approve")
    public WorkOrderDTO approve(@PathVariable Long workOrderId) {
        return service.approve(workOrderId);
    }

    @PatchMapping("/{workOrderId}/reject")
    public WorkOrderDTO reject(@PathVariable Long workOrderId) {
        return service.reject(workOrderId);
    }

    @PatchMapping("/{workOrderId}/start")
    public WorkOrderProcessResultDTO start(@PathVariable Long workOrderId) {
        return service.start(workOrderId);
    }

    @PatchMapping("/{workOrderId}/complete")
    public WorkOrderProcessResultDTO complete(@PathVariable Long workOrderId) {
        return service.complete(workOrderId);
    }
}
