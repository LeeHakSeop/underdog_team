package aaa.vehicle_p.controller;

import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.service.VehicleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Resource
    VehicleService service;

    @GetMapping
    public List<VehicleDTO> list(VehicleDTO dto) {return service.list(dto);}


}
