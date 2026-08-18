package aaa.yard_congestion_p.controller;

import aaa.yard_congestion_p.model.YardCongestionDTO;
import aaa.yard_congestion_p.service.YardCongestionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard-congestion")
public class YardCongestionController {

    @Resource
    YardCongestionService service;

    @GetMapping
    public YardCongestionDTO congestion() {
        return service.congestion();
    }
}
