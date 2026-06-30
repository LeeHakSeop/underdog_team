package aaa.carrier_p.controller;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.service.CarrierService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/carrier")
public class CarrierController {

    @Resource
    CarrierService carrierService;

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("carrierList", carrierService.list());
        return "carrier/list";
    }

    @PostMapping("/reg")
    public String reg(CarrierDTO dto) {
        carrierService.insert(dto);
        return "redirect:/carrier/list";
    }
}
