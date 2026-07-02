package aaa.container_p.controller;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.service.ContainerService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/container")
public class ContainerController {

    @Resource
    ContainerService service;

    @GetMapping
    public List<ContainerDTO> list() {
        return service.list();
    }
}
