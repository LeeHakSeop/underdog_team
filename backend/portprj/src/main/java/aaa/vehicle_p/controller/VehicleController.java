package aaa.vehicle_p.controller;

import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.service.VehicleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Resource
    VehicleService service;

    @GetMapping
    public List<VehicleDTO> list() {return service.list();}

    @GetMapping("/{vehicleId}")
    public VehicleDTO detail(@PathVariable Long vehicleId) {return service.detail(vehicleId);}

    @PostMapping
    public VehicleDTO reg(@RequestBody VehicleDTO dto) {
        service.insert(dto);
        return dto;
    }

    @PutMapping("/{vehicleId}")
    public VehicleDTO modify(@PathVariable Long vehicleId, @RequestBody VehicleDTO dto) {
        dto.setVehicleId(vehicleId);
        service.update(dto);
        return service.detail(vehicleId);
    }

    @DeleteMapping("/{vehicleId}")
    public int delete(@PathVariable Long vehicleId) {return service.delete(vehicleId);}

}
