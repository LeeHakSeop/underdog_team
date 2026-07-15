package aaa.driver_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.driver_p.model.DriverWorkOrderDTO;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleMapper;
import aaa.work_order_p.model.WorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    @Resource
    DriverMapper driverMapper;

    @Resource
    UserMapper userMapper;

    @Resource
    VehicleMapper vehicleMapper;

    @Resource
    WorkOrderMapper workOrderMapper;

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
        int result = driverMapper.update(dto);

        if (dto.getUserId() != null) {
            if (Boolean.TRUE.equals(dto.getCanEnter())) {
                userMapper.updateStatus(dto.getUserId(), "ACTIVE");
            } else if (Boolean.TRUE.equals(dto.getIsRegistered())) {
                userMapper.updateStatus(dto.getUserId(), "CARRIER_APPROVED");
            }
        }

        return result;
    }

    @Transactional
    public int delete(Long driverId) {
        DriverDTO driver = driverMapper.detail(driverId);
        if (driver == null) {
            return 0;
        }

        // 작업 이력은 유지하고 삭제되는 기사와의 참조만 해제합니다.
        workOrderMapper.clearDriverReference(driverId);
        vehicleMapper.clearDriverReference(driverId);
        if (driver.getUserId() != null) {
            vehicleMapper.clearUserReference(driver.getUserId());
        }

        int deleted = driverMapper.delete(driverId);
        if (deleted > 0 && driver.getUserId() != null) {
            userMapper.delete(driver.getUserId());
        }

        return deleted;
    }

    @Transactional
    public void approveByCarrier(Long userId) {
        UserDTO user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        if (!"DRIVER".equals(user.getRoleCode())) {
            throw new RuntimeException("기사 계정만 승인할 수 있습니다.");
        }

        driverMapper.updateApprovalByUserId(
                userId,
                true,
                false
        );

        userMapper.updateStatus(
                userId,
                "CARRIER_APPROVED"
        );
    }

    public List<DriverWorkOrderDTO> myWorkOrders(String userName) {
        return driverMapper.findWorkOrdersByUserName(userName);
    }

    public List<DriverWorkOrderDTO> myWorkOrdersByUserId(Long userId) {
        return driverMapper.findWorkOrdersByUserId(userId);
    }

    private void setDefaultValues(DriverDTO dto) {
        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(false);
        }

        if (dto.getCanEnter() == null) {
            dto.setCanEnter(false);
        }
    }
}
