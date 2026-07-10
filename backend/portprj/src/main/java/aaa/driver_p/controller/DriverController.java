package aaa.driver_p.controller;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverWorkOrderDTO;
import aaa.driver_p.service.DriverService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Resource
    DriverService driverService;

    @GetMapping("/list")
    public List<DriverDTO> list() {
        return driverService.list();
    }

    @GetMapping("/detail/{driverId}")
    public DriverDTO detail(@PathVariable Long driverId) {
        return driverService.detail(driverId);
    }

    @PostMapping("/reg")
    public DriverDTO reg(@RequestBody DriverDTO dto) {
        driverService.insert(dto);
        return dto;
    }

    @PatchMapping("/{userId}/carrier-approve")
    public void carrierApprove(@PathVariable Long userId) {
        driverService.approveByCarrier(userId);
    }

    @PutMapping("/modify/{driverId}")
    public DriverDTO modify(@PathVariable Long driverId, @RequestBody DriverDTO dto) {
        dto.setDriverId(driverId);
        driverService.update(dto);
        return driverService.detail(driverId);
    }

    @DeleteMapping("/delete/{driverId}")
    public int delete(@PathVariable Long driverId) {
        return driverService.delete(driverId);
    }

    @GetMapping("/my-work-orders")
    public List<DriverWorkOrderDTO> myWorkOrders(@RequestParam String userName) {
        return driverService.myWorkOrders(userName);
    }

    @GetMapping("/my-work-orders/user/{userId}")
    public List<DriverWorkOrderDTO> myWorkOrdersByUserId(@PathVariable Long userId) {
        return driverService.myWorkOrdersByUserId(userId);
    }
}
