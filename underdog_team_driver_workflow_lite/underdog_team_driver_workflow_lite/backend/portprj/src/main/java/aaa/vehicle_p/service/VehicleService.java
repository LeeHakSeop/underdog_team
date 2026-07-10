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

        return vehicleMapper.findTractorInfoByPlateNumber(
                plateNumber.trim()
        );
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

    /**
     * 운송사 담당자가 기사에게 트레일러를 배정한다.
     *
     * 기사 회원가입 때 등록되는 트랙터는 AuthService에서 생성하고,
     * 이 메서드에서는 운송사 배정용 트레일러만 등록한다.
     */
    @Transactional
    public int insert(VehicleDTO dto) {
        validateInsert(dto);

        String plateNumber = dto.getPlateNumber().trim();

        if (vehicleMapper.findByPlateNumber(plateNumber) != null) {
            throw new IllegalArgumentException(
                    "이미 등록된 차량번호입니다."
            );
        }

        DriverDTO driver = driverMapper.detail(dto.getDriverId());

        if (driver == null) {
            throw new IllegalArgumentException(
                    "배정할 기사 정보를 찾을 수 없습니다."
            );
        }

        if (!Boolean.TRUE.equals(driver.getIsRegistered())) {
            throw new IllegalArgumentException(
                    "운송사 가입 승인이 완료된 기사만 트레일러를 배정할 수 있습니다."
            );
        }

        if (Boolean.TRUE.equals(driver.getCanEnter())) {
            throw new IllegalArgumentException(
                    "이미 관리자 최종 승인이 완료된 기사입니다."
            );
        }

        if (driver.getCarrierId() == null
                || !driver.getCarrierId().equals(dto.getCarrierId())) {
            throw new IllegalArgumentException(
                    "해당 운송사 소속 기사에게만 트레일러를 배정할 수 있습니다."
            );
        }

        dto.setPlateNumber(plateNumber);

        // 운송사 등록 차량은 무조건 트레일러
        dto.setVehicleType("TRAILER");

        dto.setIsRegistered(false);
        dto.setVehicleStatus("승인대기");

        if (dto.getUserId() == null) {
            dto.setUserId(driver.getUserId());
        }

        return vehicleMapper.insert(dto);
    }

    @Transactional
    public int update(VehicleDTO dto) {
        validateUpdate(dto);

        VehicleDTO currentVehicle =
                vehicleMapper.detail(dto.getVehicleId());

        if (currentVehicle == null) {
            throw new IllegalArgumentException(
                    "수정할 차량 정보를 찾을 수 없습니다."
            );
        }

        String plateNumber = dto.getPlateNumber().trim();

        VehicleDTO duplicate =
                vehicleMapper.findByPlateNumber(plateNumber);

        if (duplicate != null
                && !duplicate.getVehicleId().equals(dto.getVehicleId())) {
            throw new IllegalArgumentException(
                    "이미 등록된 차량번호입니다."
            );
        }

        dto.setPlateNumber(plateNumber);

        /*
         * 기존 차량 유형을 임의로 변경하지 못하도록 한다.
         * 운송사 배정 차량은 계속 TRAILER로 유지한다.
         */
        if ("TRAILER".equalsIgnoreCase(
                currentVehicle.getVehicleType()
        )) {
            dto.setVehicleType("TRAILER");
        }

        if (dto.getIsRegistered() == null) {
            dto.setIsRegistered(
                    currentVehicle.getIsRegistered()
            );
        }

        if (dto.getVehicleStatus() == null
                || dto.getVehicleStatus().isBlank()) {
            dto.setVehicleStatus(
                    currentVehicle.getVehicleStatus()
            );
        }

        return vehicleMapper.update(dto);
    }

    @Transactional
    public int delete(Long vehicleId) {
        VehicleDTO vehicle = vehicleMapper.detail(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "삭제할 차량 정보를 찾을 수 없습니다."
            );
        }

        return vehicleMapper.delete(vehicleId);
    }

    /**
     * 관리자가 운송사가 배정한 트레일러를 최종 승인 또는 반려한다.
     */
    @Transactional
    public int updateApproval(
            Long vehicleId,
            VehicleDTO dto
    ) {
        VehicleDTO vehicle =
                vehicleMapper.detail(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "차량 정보를 찾을 수 없습니다."
            );
        }

        if (!"TRAILER".equalsIgnoreCase(
                vehicle.getVehicleType()
        )) {
            throw new IllegalArgumentException(
                    "관리자 최종 승인은 배정된 트레일러만 처리할 수 있습니다."
            );
        }

        if (vehicle.getDriverId() == null) {
            throw new IllegalArgumentException(
                    "배정 기사가 없는 트레일러입니다."
            );
        }

        DriverDTO driver =
                driverMapper.detail(vehicle.getDriverId());

        if (driver == null) {
            throw new IllegalArgumentException(
                    "배정된 기사 정보를 찾을 수 없습니다."
            );
        }

        if (!Boolean.TRUE.equals(
                driver.getIsRegistered()
        )) {
            throw new IllegalArgumentException(
                    "운송사 가입 승인이 완료되지 않은 기사입니다."
            );
        }

        if (driver.getCarrierId() == null
                || !driver.getCarrierId().equals(
                vehicle.getCarrierId()
        )) {
            throw new IllegalArgumentException(
                    "트레일러와 기사 소속 운송사 정보가 일치하지 않습니다."
            );
        }

        boolean approved =
                Boolean.TRUE.equals(dto.getIsRegistered());

        String vehicleStatus =
                approved ? "정상" : "승인거절";

        int result = vehicleMapper.updateApproval(
                vehicleId,
                approved,
                vehicleStatus
        );

        driverMapper.updateApprovalByDriverId(
                vehicle.getDriverId(),
                true,
                approved
        );

        if (driver.getUserId() != null) {
            userMapper.updateStatus(
                    driver.getUserId(),
                    approved
                            ? "ACTIVE"
                            : "CARRIER_APPROVED"
            );
        }

        return result;
    }

    private void validateInsert(VehicleDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException(
                    "차량 정보는 필수입니다."
            );
        }

        if (dto.getPlateNumber() == null
                || dto.getPlateNumber().isBlank()) {
            throw new IllegalArgumentException(
                    "트레일러 차량번호는 필수입니다."
            );
        }

        if (dto.getTonnage() == null
                || dto.getTonnage().isBlank()) {
            throw new IllegalArgumentException(
                    "톤수는 필수입니다."
            );
        }

        if (dto.getDriverId() == null) {
            throw new IllegalArgumentException(
                    "배정 기사는 필수입니다."
            );
        }

        if (dto.getCarrierId() == null) {
            throw new IllegalArgumentException(
                    "운송사 ID는 필수입니다."
            );
        }

        if (dto.getChassisNo() == null
                || dto.getChassisNo().isBlank()) {
            throw new IllegalArgumentException(
                    "트레일러 샤시 번호는 필수입니다."
            );
        }
    }

    private void validateUpdate(VehicleDTO dto) {
        if (dto == null
                || dto.getVehicleId() == null) {
            throw new IllegalArgumentException(
                    "차량 ID는 필수입니다."
            );
        }

        validateInsert(dto);
    }
}