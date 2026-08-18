package aaa.yard_map_p.controller;

import aaa.yard_map_p.model.YardMapSnapshotDTO;
import aaa.yard_map_p.service.YardMapService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard-map")
public class YardMapController {

    @Resource
    YardMapService service;

    @GetMapping("/snapshot")
    public YardMapSnapshotDTO snapshot() {
        return service.snapshot();
    }
}
