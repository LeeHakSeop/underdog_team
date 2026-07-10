package aaa.container_p.service;

import aaa.container_p.model.ContainerDTO;
import aaa.container_p.model.ContainerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContainerService {

    @Resource
    ContainerMapper mapper;

    public List<ContainerDTO> list() {
        return mapper.list();
    }

    public ContainerDTO detail(Long containerId) {
        return mapper.detail(containerId);
    }

    public int allowExit(Long containerId) {
        return mapper.updateCanExit(containerId, true);
    }

    public int blockExit(Long containerId) {
        return mapper.updateCanExit(containerId, false);
    }
}
