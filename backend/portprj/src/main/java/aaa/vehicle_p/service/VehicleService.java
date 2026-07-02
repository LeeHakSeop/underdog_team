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

    public List<VehicleDTO> list(){return mapper.list();}

    public VehicleDTO detail(Long vehicleId){return mapper.detail(vehicleId);}

    public int insert(VehicleDTO dto){
        setDefaultValues(dto);
        return mapper.insert(dto);
    }

    public int update(VehicleDTO dto){
        setDefaultValues(dto);
        return mapper.update(dto);
    }

    public int delete(Long vehicleId){return mapper.delete(vehicleId);}

    private void setDefaultValues(VehicleDTO dto) {
        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(false);
        }
        if (dto.getVehicleStatus() == null || dto.getVehicleStatus().isBlank()) {
            dto.setVehicleStatus("PENDING");
        }
    }

}
