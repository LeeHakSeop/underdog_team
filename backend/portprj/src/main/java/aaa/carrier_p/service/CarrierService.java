package aaa.carrier_p.service;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.model.CarrierMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarrierService {

    @Resource
    CarrierMapper carrierMapper;

    public List<CarrierDTO> list() {
        return carrierMapper.list();
    }

    public CarrierDTO detail(Long carrierId) {
        return carrierMapper.detail(carrierId);
    }

    public int insert(CarrierDTO dto) {
        setDefaultValues(dto);
        return carrierMapper.insert(dto);
    }

    public int update(CarrierDTO dto) {
        setDefaultValues(dto);
        return carrierMapper.update(dto);
    }

    public int delete(Long carrierId) {
        return carrierMapper.delete(carrierId);
    }

    private void setDefaultValues(CarrierDTO dto) {
        if (dto.getCarrierStatus() == null || dto.getCarrierStatus().isBlank()) {
            dto.setCarrierStatus("ACTIVE");
        }
    }
}
