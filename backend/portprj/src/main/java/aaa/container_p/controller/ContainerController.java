package aaa.container_p.controller;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.service.ContainerService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ContainerDTO create(@RequestBody ContainerDTO dto) {
        return service.insert(dto);
    }

    @PutMapping("/{containerId}")
    public ContainerDTO update(@PathVariable Long containerId, @RequestBody ContainerDTO dto) {
        return service.update(containerId, dto);
    }

    @DeleteMapping("/{containerId}")
    public void delete(@PathVariable Long containerId) {
        service.delete(containerId);
    }
}
