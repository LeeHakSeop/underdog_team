package aaa.auth_p.service;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.LoginResponseDTO;
import aaa.auth_p.model.RegisterDTO;
import aaa.carrier_p.model.CarrierMapper;
import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.filter.JwtUtil;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final UserMapper mapper;
    private final CarrierMapper carrierMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserMapper mapper,
            CarrierMapper carrierMapper,
            DriverMapper driverMapper,
            VehicleMapper vehicleMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.mapper = mapper;
        this.carrierMapper = carrierMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponseDTO login(LoginDTO dto) {
        UserDTO user = mapper.findByLoginId(dto.getLoginId());

        if (user == null || !checkPassword(dto.getPassword(), user.getPassword())) {
            throw error(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        if (dto.getRoleCode() != null
                && !dto.getRoleCode().isBlank()
                && !dto.getRoleCode().equals(user.getRoleCode())) {
            throw error(HttpStatus.UNAUTHORIZED, "선택한 로그인 유형과 계정 권한이 일치하지 않습니다.");
        }

        validateLoginStatus(user);

        if ("DRIVER".equals(user.getRoleCode())) {
            validateDriverLogin(user);
        }

        mapper.updateLastLogin(user.getUserId());

        String token = jwtUtil.createToken(user.getLoginId(), user.getRoleCode());

        LoginResponseDTO res = new LoginResponseDTO();
        res.setUserId(user.getUserId());
        res.setLoginId(user.getLoginId());
        res.setUserName(user.getUserName());
        res.setRoleCode(user.getRoleCode());
        res.setStatus(user.getStatus());
        res.setToken(token);
        res.setAccessToken(token);
        res.setTokenType("Bearer");

        return res;
    }

    private void validateLoginStatus(UserDTO user) {
        if ("PENDING".equals(user.getStatus())) {
            if ("DRIVER".equals(user.getRoleCode())) {
                throw error(HttpStatus.FORBIDDEN, "지정한 운송사의 가입 승인 후 로그인할 수 있습니다.");
            }

            if ("CARRIER".equals(user.getRoleCode())) {
                throw error(HttpStatus.FORBIDDEN, "관리자 승인 후 로그인할 수 있습니다.");
            }

            throw error(HttpStatus.FORBIDDEN, "승인 후 로그인할 수 있습니다.");
        }

        if ("CARRIER_APPROVED".equals(user.getStatus())) {
            throw error(HttpStatus.FORBIDDEN, "관리자 최종 승인 후 로그인할 수 있습니다.");
        }

        if ("REJECTED".equals(user.getStatus())) {
            throw error(HttpStatus.FORBIDDEN, "가입 또는 승인 요청이 반려되었습니다.");
        }

        if ("WITHDRAWN".equals(user.getStatus())) {
            throw error(HttpStatus.FORBIDDEN, "탈퇴 처리된 계정은 로그인할 수 없습니다.");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw error(HttpStatus.FORBIDDEN, "로그인할 수 없는 계정 상태입니다.");
        }
    }

    private void validateDriverLogin(UserDTO user) {
        DriverDTO driver = driverMapper.findByUserId(user.getUserId());

        if (driver == null) {
            throw error(HttpStatus.FORBIDDEN, "기사 정보를 찾을 수 없습니다.");
        }

        if (!Boolean.TRUE.equals(driver.getIsRegistered())) {
            throw error(HttpStatus.FORBIDDEN, "지정한 운송사의 가입 승인 후 로그인할 수 있습니다.");
        }

        if (!Boolean.TRUE.equals(driver.getCanEnter())) {
            throw error(HttpStatus.FORBIDDEN, "관리자 최종 승인 후 로그인할 수 있습니다.");
        }
    }

    private boolean checkPassword(String inputPassword, String savedPassword) {
        if (inputPassword == null || savedPassword == null) {
            return false;
        }

        if (savedPassword.startsWith("$2a$")
                || savedPassword.startsWith("$2b$")
                || savedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(inputPassword, savedPassword);
        }

        return inputPassword.equals(savedPassword);
    }

    public boolean isLoginIdAvailable(String loginId) {
        return mapper.countByLoginId(normalizeLoginId(loginId)) == 0;
    }

    @Transactional
    public int register(RegisterDTO dto) {
        validateRegister(dto);

        if (!isLoginIdAvailable(dto.getLoginId())) {
            throw error(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }

        if ("CARRIER".equals(dto.getRoleCode())) {
            validateCarrierRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            validateDriverRegister(dto);
            validateDriverTractor(dto);
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setStatus("ADMIN".equals(dto.getRoleCode()) ? "ACTIVE" : "PENDING");

        int result = mapper.insertUser(dto);

        if ("CARRIER".equals(dto.getRoleCode())) {
            carrierMapper.insertFromRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            driverMapper.insertFromRegister(dto);
            insertDriverTractor(dto);
        }

        return result;
    }

    public List<UserDTO> findUsers() {
        List<UserDTO> users = mapper.findAll();

        if (users != null) {
            users.forEach(user -> user.setPassword(null));
        }

        return users;
    }

    @Transactional
    public UserDTO updateStatus(Long userId, String status) {
        if (!List.of("PENDING", "CARRIER_APPROVED", "ACTIVE", "REJECTED").contains(status)) {
            throw error(HttpStatus.BAD_REQUEST, "상태값이 올바르지 않습니다.");
        }

        UserDTO user = mapper.findById(userId);

        if (user == null) {
            throw error(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        if ("ACTIVE".equals(status)) {
            approveRelatedAccount(user);
        }

        if ("REJECTED".equals(status)) {
            rejectRelatedAccount(user);
        }

        mapper.updateStatus(userId, status);

        UserDTO updatedUser = mapper.findById(userId);

        if (updatedUser != null) {
            updatedUser.setPassword(null);
        }

        return updatedUser;
    }

    private void approveRelatedAccount(UserDTO user) {
        if ("CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(user.getUserId(), "APPROVED");
        }

        if ("DRIVER".equals(user.getRoleCode())
                && !"CARRIER_APPROVED".equals(user.getStatus())) {
            throw error(HttpStatus.CONFLICT, "기사는 운송사 승인과 트레일러 배정 후 최종 승인할 수 있습니다.");
        }

        if ("DRIVER".equals(user.getRoleCode())) {
            DriverDTO driver = driverMapper.findByUserId(user.getUserId());
            VehicleDTO vehicle = driver == null || driver.getDriverId() == null
                    ? null
                    : vehicleMapper.findByDriverId(driver.getDriverId());

            if (driver == null
                    || !Boolean.TRUE.equals(driver.getIsRegistered())
                    || vehicle == null
                    || !Boolean.TRUE.equals(vehicle.getIsRegistered())) {
                throw error(HttpStatus.CONFLICT, "운송사 승인과 차량 관리자 승인 후 최종 승인할 수 있습니다.");
            }

            int updated = driverMapper.updateApprovalByUserId(user.getUserId(), true, true);
            if (updated != 1) {
                throw error(HttpStatus.CONFLICT, "기사 출입 승인 처리에 실패했습니다.");
            }
        }
    }

    private void rejectRelatedAccount(UserDTO user) {
        if ("CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(user.getUserId(), "REJECTED");
        }

        if ("DRIVER".equals(user.getRoleCode())) {
            driverMapper.updateApprovalByUserId(user.getUserId(), false, false);
        }
    }

    private void validateRegister(RegisterDTO dto) {
        dto.setLoginId(normalizeLoginId(dto.getLoginId()));

        if (dto.getPassword() == null || dto.getPassword().length() < 4) {
            throw error(HttpStatus.BAD_REQUEST, "비밀번호는 4자 이상 입력하세요.");
        }

        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "이름은 필수입니다.");
        }

        if (!List.of("ADMIN", "CARRIER", "DRIVER").contains(dto.getRoleCode())) {
            throw error(HttpStatus.BAD_REQUEST, "역할은 ADMIN, CARRIER, DRIVER 중 하나여야 합니다.");
        }
    }

    private String normalizeLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "아이디는 필수입니다.");
        }

        return loginId.trim();
    }

    private void validateCarrierRegister(RegisterDTO dto) {
        if (dto.getCarrierName() == null || dto.getCarrierName().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "운송사명은 필수입니다.");
        }

        if (dto.getManagerName() == null || dto.getManagerName().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "운송사 담당자명은 필수입니다.");
        }

        if (dto.getCarrierContact() == null || dto.getCarrierContact().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "운송사 연락처는 필수입니다.");
        }
    }

    private void validateDriverRegister(RegisterDTO dto) {
        if (dto.getDriverName() == null || dto.getDriverName().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "기사명은 필수입니다.");
        }

        if (dto.getDriverContact() == null || dto.getDriverContact().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "기사 연락처는 필수입니다.");
        }

        if (dto.getCarrierId() == null) {
            throw error(HttpStatus.BAD_REQUEST, "소속 운송사는 필수입니다.");
        }

        if (carrierMapper.countApprovedForSignup(dto.getCarrierId()) == 0) {
            throw error(HttpStatus.BAD_REQUEST, "승인된 운송사만 선택할 수 있습니다.");
        }
    }

    private void validateDriverTractor(RegisterDTO dto) {
        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "트랙터 차량번호는 필수입니다.");
        }

        if (dto.getTonnage() == null || dto.getTonnage().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "트랙터 축 형식은 필수입니다.");
        }

    }

    private void insertDriverTractor(RegisterDTO dto) {
        String plateNumber = dto.getPlateNumber().trim();

        if (vehicleMapper.findByPlateNumber(plateNumber) != null) {
            throw error(HttpStatus.CONFLICT, "이미 등록된 차량번호입니다.");
        }

        VehicleDTO vehicle = new VehicleDTO();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType("TRACTOR");
        vehicle.setTonnage(dto.getTonnage().trim());
        vehicle.setIsRegistered(false);
        vehicle.setVehicleStatus("승인대기");
        vehicle.setTractorNo(normalizeSystemTractorNo(dto));
        vehicle.setChassisNo(normalizeSystemChassisNo(dto));
        vehicle.setDriverId(dto.getDriverId());
        vehicle.setCarrierId(dto.getCarrierId());
        vehicle.setUserId(dto.getUserId());

        vehicleMapper.insert(vehicle);
        if (vehicle.getVehicleId() != null) {
            vehicleMapper.insertTractorSubtype(vehicle.getVehicleId());
        }
    }

    private String normalizeSystemChassisNo(RegisterDTO dto) {
        if (dto.getChassisNo() != null && !dto.getChassisNo().isBlank()) {
            return dto.getChassisNo().trim();
        }

        return "AUTO-" + dto.getUserId() + "-" + dto.getPlateNumber().trim().replaceAll("\\s+", "");
    }

    private String normalizeSystemTractorNo(RegisterDTO dto) {
        if (dto.getTractorNo() != null && !dto.getTractorNo().isBlank()) {
            return dto.getTractorNo().trim();
        }

        return "TR-" + dto.getUserId() + "-" + dto.getPlateNumber().trim().replaceAll("\\s+", "");
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
