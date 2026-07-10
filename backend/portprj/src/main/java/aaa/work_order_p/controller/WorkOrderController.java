package aaa.work_order_p.controller;

import aaa.work_order_p.model.TrailerWorkInfoDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.work_order_p.model.WorkOrderProcessResultDTO;
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

    @GetMapping("/{workOrderId}")
    public WorkOrderDTO detail(@PathVariable Long workOrderId) {
        return service.detail(workOrderId);
    }

    @PostMapping
    public WorkOrderDTO create(@RequestBody WorkOrderDTO dto) {
        service.insert(dto);
        return dto;
    }

    @GetMapping("/trailer-info/{vehicleId}")
    public TrailerWorkInfoDTO trailerInfo(@PathVariable Long vehicleId) {
        return service.findTrailerWorkInfo(vehicleId);
    }

<<<<<<< HEAD
    @PatchMapping("/{workOrderId}/approve")
    public WorkOrderDTO approve(@PathVariable Long workOrderId) {
        return service.approve(workOrderId);
    }

    @PatchMapping("/{workOrderId}/start")
    public WorkOrderDTO start(@PathVariable Long workOrderId) {
=======
    @PatchMapping("/{workOrderId}/start")
    public WorkOrderProcessResultDTO start(@PathVariable Long workOrderId) {
>>>>>>> origin/pjh
        return service.start(workOrderId);
    }

    @PatchMapping("/{workOrderId}/complete")
<<<<<<< HEAD
    public WorkOrderDTO complete(@PathVariable Long workOrderId) {
=======
    public WorkOrderProcessResultDTO complete(@PathVariable Long workOrderId) {
>>>>>>> origin/pjh
        return service.complete(workOrderId);
    }
}
