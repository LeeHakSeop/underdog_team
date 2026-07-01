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
}
