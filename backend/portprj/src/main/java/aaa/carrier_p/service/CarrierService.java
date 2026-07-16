package aaa.carrier_p.service;

import aaa.carrier_p.model.CarrierDTO;
import aaa.carrier_p.model.CarrierMapper;
import aaa.user_p.model.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarrierService {

    @Resource
    CarrierMapper carrierMapper;

    @Resource
    UserMapper userMapper;

    public List<CarrierDTO> list() {
        return carrierMapper.list();
    }

    public CarrierDTO detail(Long carrierId) {
        return carrierMapper.detail(carrierId);
    }

    public int insert(CarrierDTO dto) {
        setDefaultStatus(dto);
        return carrierMapper.insert(dto);
    }

    public int update(CarrierDTO dto) {
        setDefaultStatus(dto);
        return carrierMapper.update(dto);
    }

    @Transactional
    public int delete(Long carrierId) {
        CarrierDTO carrier = carrierMapper.detail(carrierId);
        if (carrier == null) {
            return 0;
        }

        List<Long> driverUserIds = carrierMapper.findDriverUserIds(carrierId);

        // 기사·차량을 참조하는 운영 이력은 보존하고, 삭제 대상과의 연결만 해제합니다.
        carrierMapper.clearWorkOrderDriverReferences(carrierId);
        carrierMapper.detachVehicles(carrierId);
        carrierMapper.deleteDrivers(carrierId);

        int deleted = carrierMapper.delete(carrierId);

        driverUserIds.forEach(userMapper::delete);
        if (carrier.getUserId() != null) {
            userMapper.delete(carrier.getUserId());
        }

        return deleted;
    }

    private void setDefaultStatus(CarrierDTO dto) {
        if (dto.getCarrierStatus() == null || dto.getCarrierStatus().isBlank()) {
            dto.setCarrierStatus("ACTIVE");
        }
    }
}
