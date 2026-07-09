package aaa.vehicle_p.controller;

import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.service.VehicleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {
    @Resource
    VehicleService vehicleService;

    @GetMapping("/list")
    public List<VehicleDTO> list() {
        return vehicleService.list();
    }

    @GetMapping("/detail/{vehicleId}")
    public VehicleDTO detail(@PathVariable Long vehicleId) {
        return vehicleService.detail(vehicleId);
    }

    @GetMapping("/tractor-info/{plateNumber}")
    public TractorVehicleInfoDTO tractorInfo(@PathVariable String plateNumber) {
        return vehicleService.findTractorInfo(plateNumber);
    }

    @PostMapping("/reg")
    public VehicleDTO reg(@RequestBody VehicleDTO dto) {
        vehicleService.insert(dto);
        return dto;
    }

    @PutMapping("/modify/{vehicleId}")
    public VehicleDTO modify(@PathVariable Long vehicleId, @RequestBody VehicleDTO dto) {
        dto.setVehicleId(vehicleId);
        vehicleService.update(dto);
        return vehicleService.detail(vehicleId);
    }

    @DeleteMapping("/delete/{vehicleId}")
    public int delete(@PathVariable Long vehicleId) {
        return vehicleService.delete(vehicleId);
    }
}
