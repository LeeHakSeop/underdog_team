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

        return vehicleMapper.findTractorInfoByPlateNumber(plateNumber.trim());
    }

    public List<VehicleDTO> findByCarrierId(Long carrierId) {
        if (carrierId == null) {
            throw new IllegalArgumentException("운송사 ID는 필수입니다.");
        }

        return vehicleMapper.findByCarrierId(carrierId);
    }

    public VehicleDTO findByDriverId(Long driverId) {
        if (driverId == null) {
            throw new IllegalArgumentException("기사 ID는 필수입니다.");
        }

        return vehicleMapper.findByDriverId(driverId);
    }

    @Transactional
    public int insert(VehicleDTO dto) {
        validateInsert(dto);

        String plateNumber = dto.getPlateNumber().trim();
        if (vehicleMapper.findByPlateNumber(plateNumber) != null) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        DriverDTO driver = driverMapper.detail(dto.getDriverId());
        if (driver == null) {
            throw new IllegalArgumentException("배정할 기사 정보를 찾을 수 없습니다.");
        }

        if (!Boolean.TRUE.equals(driver.getIsRegistered())) {
            throw new IllegalArgumentException("운송사 가입 승인이 완료된 기사만 트레일러를 배정할 수 있습니다.");
        }

        if (Boolean.TRUE.equals(driver.getCanEnter())) {
            throw new IllegalArgumentException("이미 관리자 최종 승인이 완료된 기사입니다.");
        }

        if (driver.getCarrierId() == null || !driver.getCarrierId().equals(dto.getCarrierId())) {
            throw new IllegalArgumentException("해당 운송사 소속 기사에게만 트레일러를 배정할 수 있습니다.");
        }

        dto.setPlateNumber(plateNumber);
        dto.setVehicleType("TRAILER");
        dto.setIsRegistered(false);
        dto.setVehicleStatus("승인대기");

        if (dto.getUserId() == null) {
            dto.setUserId(driver.getUserId());
        }

        int inserted = vehicleMapper.insert(dto);
        if (inserted == 1 && dto.getVehicleId() != null) {
            vehicleMapper.insertTrailerSubtype(dto.getVehicleId());
        }

        return inserted;
    }

    @Transactional
    public int update(VehicleDTO dto) {
        validateUpdate(dto);

        VehicleDTO currentVehicle = vehicleMapper.detail(dto.getVehicleId());
        if (currentVehicle == null) {
            currentVehicle = dto;
        }

        String plateNumber = dto.getPlateNumber().trim();
        VehicleDTO duplicate = vehicleMapper.findByPlateNumber(plateNumber);
        if (duplicate != null && !duplicate.getVehicleId().equals(dto.getVehicleId())) {
            throw new IllegalArgumentException("이미 등록된 차량번호입니다.");
        }

        dto.setPlateNumber(plateNumber);
        dto.setVehicleType(currentVehicle.getVehicleType());

        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(currentVehicle.getIsRegistered());
        }

        if (dto.getVehicleStatus() == null || dto.getVehicleStatus().isBlank()) {
            dto.setVehicleStatus(currentVehicle.getVehicleStatus());
        }

        return vehicleMapper.update(dto);
    }

    @Transactional
    public int delete(Long vehicleId) {
        VehicleDTO vehicle = vehicleMapper.detail(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("삭제할 차량 정보를 찾을 수 없습니다.");
        }

        return vehicleMapper.delete(vehicleId);
    }

    @Transactional
    public int updateApproval(Long vehicleId, VehicleDTO dto) {
        VehicleDTO vehicle = vehicleMapper.detail(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException("차량 정보를 찾을 수 없습니다.");
        }

        if (vehicle.getVehicleType() != null
                && !vehicle.getVehicleType().isBlank()
                && !isApprovalTargetVehicle(vehicle.getVehicleType())) {
            throw new IllegalArgumentException("관리자 최종 승인은 배정된 트랙터 또는 트레일러만 처리할 수 있습니다.");
        }

        if (vehicle.getDriverId() == null) {
            throw new IllegalArgumentException("배정 기사가 없는 차량입니다.");
        }

        DriverDTO driver = driverMapper.detail(vehicle.getDriverId());
        if (driver == null) {
            throw new IllegalArgumentException("배정된 기사 정보를 찾을 수 없습니다.");
        }

        if (Boolean.FALSE.equals(driver.getIsRegistered())) {
            throw new IllegalArgumentException("운송사 가입 승인이 완료되지 않은 기사입니다.");
        }

        if (driver.getCarrierId() != null
                && vehicle.getCarrierId() != null
                && !driver.getCarrierId().equals(vehicle.getCarrierId())) {
            throw new IllegalArgumentException("차량과 기사 소속 운송사 정보가 일치하지 않습니다.");
        }

        boolean approved = Boolean.TRUE.equals(dto.getIsRegistered());
        String vehicleStatus = approved ? "정상" : "승인거절";

        int updated = vehicleMapper.updateApproval(vehicleId, approved, vehicleStatus);
        if (updated != 1) {
            return updated;
        }

        driverMapper.updateApprovalByDriverId(driver.getDriverId(), true, approved);

        if (driver.getUserId() != null) {
            userMapper.updateStatus(driver.getUserId(), approved ? "ACTIVE" : "CARRIER_APPROVED");
        }

        return updated;
    }

    private void validateInsert(VehicleDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("차량 정보는 필수입니다.");
        }

        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw new IllegalArgumentException("트레일러 차량번호는 필수입니다.");
        }

        if (dto.getTonnage() == null || dto.getTonnage().isBlank()) {
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

        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw new IllegalArgumentException("차량번호는 필수입니다.");
        }
    }

    private boolean isTrailer(String vehicleType) {
        return "TRAILER".equalsIgnoreCase(vehicleType) || "트레일러".equals(vehicleType);
    }

    private boolean isTractor(String vehicleType) {
        return "TRACTOR".equalsIgnoreCase(vehicleType) || "트랙터".equals(vehicleType);
    }

    private boolean isApprovalTargetVehicle(String vehicleType) {
        return isTractor(vehicleType) || isTrailer(vehicleType);
    }
}
