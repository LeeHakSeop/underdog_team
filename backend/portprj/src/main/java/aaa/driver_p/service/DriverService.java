package aaa.driver_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.driver_p.model.DriverWorkOrderDTO;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
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

        UserDTO currentUser = dto.getUserId() == null
                ? null
                : userMapper.findById(dto.getUserId());

        if (dto.getUserId() != null
                && (currentUser == null || !"WITHDRAWN".equals(currentUser.getStatus()))) {
            if (Boolean.TRUE.equals(dto.getCanEnter())) {
                userMapper.updateStatus(dto.getUserId(), "ACTIVE");
            } else if (Boolean.TRUE.equals(dto.getIsRegistered())) {
                userMapper.updateStatus(dto.getUserId(), "CARRIER_APPROVED");
            }
        }

        return result;
    }

    @Transactional
    public int withdraw(Long driverId) {
        DriverDTO driver = driverMapper.detail(driverId);
        if (driver == null) {
            return 0;
        }

        // 기존 작업·출입 기록은 유지하고 회원 상태만 변경합니다.
        if (driver.getUserId() == null) {
            throw new RuntimeException("회원 계정이 연결되지 않은 기사입니다.");
        }

        if ("WITHDRAWN".equals(driver.getUserStatus())) {
            return 1;
        }

        int driverUpdated = driverMapper.updateApprovalByDriverId(
                driverId,
                false,
                false
        );
        int userUpdated = userMapper.updateStatus(
                driver.getUserId(),
                "WITHDRAWN"
        );

        if (driverUpdated != 1 || userUpdated != 1) {
            throw new RuntimeException("기사 탈퇴 처리에 실패했습니다.");
        }

        return 1;
    }

    @Transactional
    public int reactivate(Long driverId) {
        DriverDTO driver = driverMapper.detail(driverId);
        if (driver == null) {
            return 0;
        }

        if (driver.getUserId() == null) {
            throw new RuntimeException("회원 계정이 연결되지 않은 기사입니다.");
        }

        if (!"WITHDRAWN".equals(driver.getUserStatus())) {
            throw new RuntimeException("탈퇴 처리된 기사만 재활성화할 수 있습니다.");
        }

        int driverUpdated = driverMapper.updateApprovalByDriverId(
                driverId,
                true,
                false
        );
        int userUpdated = userMapper.updateStatus(
                driver.getUserId(),
                "CARRIER_APPROVED"
        );

        if (driverUpdated != 1 || userUpdated != 1) {
            throw new RuntimeException("기사 재활성화에 실패했습니다.");
        }

        return 1;
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
