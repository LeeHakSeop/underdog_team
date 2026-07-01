package aaa.carrier_p.controller;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.service.CarrierService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://200.200.200.66:5173"})
@RestController
@RequestMapping("/api/carrier")
public class CarrierController {

    @Resource
    CarrierService carrierService;

    @GetMapping
    public List<CarrierDTO> list(){
        return carrierService.list();
    }

    @GetMapping("/{carrierId}")
    public CarrierDTO detail(@PathVariable Long carrierId){
        return carrierService.detail(carrierId);
    }

    @PostMapping
    public CarrierDTO reg(@RequestBody CarrierDTO dto){
        carrierService.insert(dto);
        return dto;
    }

    @PutMapping("/{carrierId}")
    public CarrierDTO modify(@PathVariable Long carrierId, @RequestBody CarrierDTO dto){
        dto.setCarrierId(carrierId);
        carrierService.update(dto);
        return carrierService.detail(carrierId);
    }

    @DeleteMapping("/{carrierId}")
    public int delete(@PathVariable Long carrierId){
        return carrierService.delete(carrierId);
    }
}
