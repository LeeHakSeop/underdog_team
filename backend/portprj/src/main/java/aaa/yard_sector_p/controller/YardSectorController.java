package aaa.yard_sector_p.controller;

import aaa.yard_sector_p.model.YardSectorDTO;
import aaa.yard_sector_p.service.YardSectorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yard-sector")
public class YardSectorController {

    @Resource
    YardSectorService service;

    @GetMapping
    public List<YardSectorDTO> list() {
        return service.list();
    }
}
