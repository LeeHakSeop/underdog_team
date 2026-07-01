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

    public int insert(CarrierDTO dto) {
        if (dto.getCarrierStatus() == null || dto.getCarrierStatus().isBlank()) {
            dto.setCarrierStatus("ACTIVE");
        }
        return carrierMapper.insert(dto);
    }
}
