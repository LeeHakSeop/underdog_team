package aaa.vehicle_p.service;

import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    public List<VehicleDTO> list() {
        return vehicleMapper.list();
    }

    public VehicleDTO detail(Long vehicleId) {
        return vehicleMapper.detail(vehicleId);
    }

    public TractorVehicleInfoDTO findTractorInfo(String plateNumber) {
        return vehicleMapper.findTractorInfoByPlateNumber(plateNumber);
    }

    public int insert(VehicleDTO dto) {
        setDefaultValues(dto);

        if (vehicleMapper.findByPlateNumber(dto.getPlateNumber()) != null) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        return vehicleMapper.insert(dto);
    }

    public int update(VehicleDTO dto) {
        setDefaultValues(dto);

        VehicleDTO old = vehicleMapper.findByPlateNumber(dto.getPlateNumber());
        if (old != null && !old.getVehicleId().equals(dto.getVehicleId())) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        return vehicleMapper.update(dto);
    }

    public int delete(Long vehicleId) {
        return vehicleMapper.delete(vehicleId);
    }

    private void setDefaultValues(VehicleDTO dto) {
        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(true);
        }

        if (dto.getVehicleStatus() == null || dto.getVehicleStatus().isBlank()) {
            dto.setVehicleStatus("ACTIVE");
        }
    }
}
