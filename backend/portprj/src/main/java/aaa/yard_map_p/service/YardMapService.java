package aaa.yard_map_p.service;

import aaa.yard_map_p.model.YardMapMapper;
import aaa.yard_map_p.model.YardMapSnapshotDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class YardMapService {

    @Resource
    YardMapMapper mapper;

    public YardMapSnapshotDTO snapshot() {
        YardMapSnapshotDTO dto = new YardMapSnapshotDTO();
        dto.setSectors(mapper.sectors());
        dto.setGates(mapper.gates());
        dto.setVehicles(mapper.vehicles());
        return dto;
    }
}
