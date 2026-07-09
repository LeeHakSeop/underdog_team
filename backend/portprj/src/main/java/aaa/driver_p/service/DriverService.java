package aaa.driver_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.driver_p.model.DriverWorkOrderDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    @Resource
    DriverMapper driverMapper;

    public List<DriverDTO> list() {
        return driverMapper.list();
    }

    public DriverDTO detail(Long driverId) {
        return driverMapper.detail(driverId);
    }

    public int insert(DriverDTO dto) {
        setDefaultValues(dto);
        return driverMapper.insert(dto);
    }

    public int update(DriverDTO dto) {
        setDefaultValues(dto);
        return driverMapper.update(dto);
    }

    public int delete(Long driverId) {
        return driverMapper.delete(driverId);
    }

    public List<DriverWorkOrderDTO> myWorkOrders(String userName) {
        return driverMapper.findWorkOrdersByUserName(userName);
    }

    private void setDefaultValues(DriverDTO dto) {
        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(true);
        }
        if (dto.getCanEnter() == null) {
            dto.setCanEnter(true);
        }
    }
}
