package aaa.vehicle_p.service;

import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper mapper;

    public List<VehicleDTO> list(VehicleDTO dto){return mapper.list(dto);}

}
