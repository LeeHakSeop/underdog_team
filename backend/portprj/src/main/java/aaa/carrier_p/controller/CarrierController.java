package aaa.carrier_p.controller;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.service.CarrierService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://200.200.200.66:5173")
@RestController
@RequestMapping("/api/carrier")
public class CarrierController {

    @Resource
    CarrierService carrierService;

    @GetMapping("/list")
    public List<CarrierDTO> list(){
        return carrierService.list();
    }

    @PostMapping("/reg")
    public CarrierDTO reg(@RequestBody CarrierDTO dto){
        carrierService.insert(dto);
        return dto;
    }
}
