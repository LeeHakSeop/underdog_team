package aaa.vehicle_p.service;

import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.TractorVehicleInfoDTO;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    @Resource
    DriverMapper driverMapper;

    @Resource
    UserMapper userMapper;

    public List<VehicleDTO> list() {
        return vehicleMapper.list();
    }

    public VehicleDTO detail(Long vehicleId) {
        return vehicleMapper.detail(vehicleId);
    }

    public TractorVehicleInfoDTO findTractorInfo(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            throw new IllegalArgumentException("차량번호는 필수입니다.");
        }

        return vehicleMapper.findTractorInfoByPlateNumber(plateNumber);
    }

    public List<VehicleDTO> findByCarrierId(Long carrierId) {
        return vehicleMapper.findByCarrierId(carrierId);
    }

    public VehicleDTO findByDriverId(Long driverId) {
        return vehicleMapper.findByDriverId(driverId);
    }

    @Transactional
    public int insert(VehicleDTO dto) {
        validateInsert(dto);

        if (vehicleMapper.findByPlateNumber(dto.getPlateNumber()) != null) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        dto.setIsRegistered(false);
        dto.setVehicleStatus("승인대기");

        return vehicleMapper.insert(dto);
    }

    @Transactional
    public int update(VehicleDTO dto) {
        validateUpdate(dto);

        VehicleDTO old = vehicleMapper.findByPlateNumber(dto.getPlateNumber());

        if (old != null && !old.getVehicleId().equals(dto.getVehicleId())) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(false);
        }

        if (dto.getVehicleStatus() == null || dto.getVehicleStatus().isBlank()) {
            dto.setVehicleStatus("승인대기");
        }

        return vehicleMapper.update(dto);
    }

    public int delete(Long vehicleId) {
        return vehicleMapper.delete(vehicleId);
    }

    @Transactional
    public int updateApproval(Long vehicleId, VehicleDTO dto) {
        VehicleDTO vehicle = vehicleMapper.detail(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException("차량 정보를 찾을 수 없습니다.");
        }

        if (vehicle.getDriverId() == null) {
            throw new IllegalArgumentException("배정 기사가 없는 차량입니다.");
        }

        DriverDTO driver = driverMapper.detail(vehicle.getDriverId());

        if (driver == null) {
            throw new IllegalArgumentException("배정된 기사 정보를 찾을 수 없습니다.");
        }

        boolean approved = Boolean.TRUE.equals(dto.getIsRegistered());
        String vehicleStatus = approved ? "정상" : "승인거절";

        int updated = vehicleMapper.updateApproval(
                vehicleId,
                approved,
                vehicleStatus
        );

        if (updated != 1) {
            return updated;
        }

        driverMapper.updateApprovalByDriverId(
                driver.getDriverId(),
                true,
                approved
        );

        if (driver.getUserId() != null) {
            userMapper.updateStatus(
                    driver.getUserId(),
                    approved ? "ACTIVE" : "CARRIER_APPROVED"
            );
        }

        return updated;
    }

    private void validateInsert(VehicleDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("차량 정보는 필수입니다.");
        }

        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw new IllegalArgumentException("차량번호는 필수입니다.");
        }

        if (dto.getVehicleType() == null || dto.getVehicleType().isBlank()) {
            throw new IllegalArgumentException("차량종류는 필수입니다.");
        }

        if (!isTractor(dto.getVehicleType())
                && (dto.getTonnage() == null || dto.getTonnage().isBlank())) {
            throw new IllegalArgumentException("톤수는 필수입니다.");
        }

        if (dto.getDriverId() == null) {
            throw new IllegalArgumentException("배정 기사는 필수입니다.");
        }

        if (dto.getCarrierId() == null) {
            throw new IllegalArgumentException("운송사 ID는 필수입니다.");
        }
    }

    private void validateUpdate(VehicleDTO dto) {
        if (dto == null || dto.getVehicleId() == null) {
            throw new IllegalArgumentException("차량 ID는 필수입니다.");
        }

        validateInsert(dto);
    }

    private boolean isTractor(String vehicleType) {
        return "TRACTOR".equalsIgnoreCase(vehicleType)
                || "트랙터".equals(vehicleType);
    }
}
