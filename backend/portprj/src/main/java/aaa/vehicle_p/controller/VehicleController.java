package aaa.vehicle_p.controller;

import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.service.VehicleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://200.200.200.66:5173"
})
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

    @GetMapping("/carrier/{carrierId}")
    public List<VehicleDTO> listByCarrier(@PathVariable Long carrierId) {
        return vehicleService.findByCarrierId(carrierId);
    }

    @GetMapping("/driver/{driverId}")
    public VehicleDTO findByDriver(@PathVariable Long driverId) {
        return vehicleService.findByDriverId(driverId);
    }

    @PostMapping("/reg")
    public VehicleDTO reg(@RequestBody VehicleDTO dto) {
        vehicleService.insert(dto);
        return dto;
    }

    @PatchMapping("/{vehicleId}/approval")
    public VehicleDTO updateApproval(
            @PathVariable Long vehicleId,
            @RequestBody VehicleDTO dto
    ) {
        vehicleService.updateApproval(vehicleId, dto);
        return vehicleService.detail(vehicleId);
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