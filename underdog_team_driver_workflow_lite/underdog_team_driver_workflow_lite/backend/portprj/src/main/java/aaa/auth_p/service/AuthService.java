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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        if (dto.getRoleCode() != null
                && !dto.getRoleCode().isBlank()
                && !dto.getRoleCode().equals(user.getRoleCode())) {
            throw new RuntimeException("선택한 로그인 유형이 계정 권한과 다릅니다.");
        }

        validateLoginStatus(user);

        if ("DRIVER".equals(user.getRoleCode())) {
            validateDriverLogin(user);
        }

        mapper.updateLastLogin(user.getUserId());

        String token = jwtUtil.createToken(
                user.getLoginId(),
                user.getRoleCode()
        );

        LoginResponseDTO res = new LoginResponseDTO();
        res.setUserId(user.getUserId());
        res.setLoginId(user.getLoginId());
        res.setUserName(user.getUserName());
        res.setRoleCode(user.getRoleCode());
        res.setStatus(user.getStatus());

        /*
         * 기존 프론트와 origin/main 양쪽 호환용.
         * LoginResponseDTO에도 token, accessToken, tokenType 필드를 모두 둔다.
         */
        res.setToken(token);
        res.setAccessToken(token);
        res.setTokenType("Bearer");

        return res;
    }

    private void validateLoginStatus(UserDTO user) {
        if ("PENDING".equals(user.getStatus())) {
            if ("DRIVER".equals(user.getRoleCode())) {
                throw new RuntimeException("지정한 운송사의 가입 승인 후 로그인할 수 있습니다.");
            }

            if ("CARRIER".equals(user.getRoleCode())) {
                throw new RuntimeException("관리자 승인 후 로그인할 수 있습니다.");
            }

            throw new RuntimeException("승인 후 로그인할 수 있습니다.");
        }

        if ("CARRIER_APPROVED".equals(user.getStatus())) {
            throw new RuntimeException("트레일러 배정 및 관리자 최종 승인 후 로그인할 수 있습니다.");
        }

        if ("REJECTED".equals(user.getStatus())) {
            throw new RuntimeException("가입 또는 승인 요청이 반려되었습니다.");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("로그인할 수 없는 계정 상태입니다.");
        }
    }

    private void validateDriverLogin(UserDTO user) {
        DriverDTO driver = driverMapper.findByUserId(user.getUserId());

        if (driver == null) {
            throw new RuntimeException("기사 정보를 찾을 수 없습니다.");
        }

        if (!Boolean.TRUE.equals(driver.getIsRegistered())) {
            throw new RuntimeException("지정한 운송사의 가입 승인 후 로그인할 수 있습니다.");
        }

        if (!Boolean.TRUE.equals(driver.getCanEnter())) {
            throw new RuntimeException("트레일러 배정 및 관리자 최종 승인 후 로그인할 수 있습니다.");
        }
    }

    /**
     * 기존 평문 테스트 계정과 BCrypt 계정을 모두 처리한다.
     * 기존 평문 계정을 모두 BCrypt로 전환한 뒤에는 평문 비교 부분을 제거하는 것이 좋다.
     */
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

    @Transactional
    public int register(RegisterDTO dto) {
        validateRegister(dto);

        if (mapper.countByLoginId(dto.getLoginId()) > 0) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        /*
         * 관련 테이블 입력 전에 역할별 필수값을 검사한다.
         * 중간 INSERT 후 예외가 발생하더라도 @Transactional로 롤백된다.
         */
        if ("CARRIER".equals(dto.getRoleCode())) {
            validateCarrierRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            validateDriverRegister(dto);
            validateDriverTractor(dto);

            if (vehicleMapper.findByPlateNumber(dto.getPlateNumber()) != null) {
                throw new RuntimeException("이미 등록된 차량번호입니다.");
            }
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setStatus(
                "ADMIN".equals(dto.getRoleCode())
                        ? "ACTIVE"
                        : "PENDING"
        );

        int result = mapper.insertUser(dto);

        if ("CARRIER".equals(dto.getRoleCode())) {
            carrierMapper.insertFromRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            driverMapper.insertFromRegister(dto);

            DriverDTO driver = driverMapper.findByUserId(dto.getUserId());

            if (driver == null || driver.getDriverId() == null) {
                throw new RuntimeException("기사 정보 생성에 실패했습니다.");
            }

            VehicleDTO tractor = new VehicleDTO();
            tractor.setPlateNumber(dto.getPlateNumber().trim());
            tractor.setVehicleType("TRACTOR");
            tractor.setTonnage(dto.getTonnage());
            tractor.setIsRegistered(false);
            tractor.setVehicleStatus("승인대기");
            tractor.setTractorNo(dto.getTractorNo());
            tractor.setChassisNo(dto.getChassisNo());
            tractor.setDriverId(driver.getDriverId());
            tractor.setCarrierId(dto.getCarrierId());
            tractor.setUserId(dto.getUserId());

            vehicleMapper.insert(tractor);
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

    /**
     * 관리자 계정 상태 변경.
     *
     * 관리자 화면에서는:
     * - 운송사 PENDING 계정만 ACTIVE 또는 REJECTED 처리
     * - 기사의 최종 승인은 VehicleService.updateApproval()에서 처리
     */
    @Transactional
    public UserDTO updateStatus(Long userId, String status) {
        if (!List.of(
                "PENDING",
                "CARRIER_APPROVED",
                "ACTIVE",
                "REJECTED"
        ).contains(status)) {
            throw new RuntimeException("상태값이 올바르지 않습니다.");
        }

        UserDTO user = mapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
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
            carrierMapper.updateStatusByUserId(
                    user.getUserId(),
                    "APPROVED"
            );
        }

        /*
         * 기사 최종 승인은 여기서 처리하지 않는다.
         *
         * 기사 흐름:
         * PENDING
         * → 운송사 승인: CARRIER_APPROVED
         * → 트레일러 배정
         * → 관리자 차량 최종 승인: ACTIVE
         */
        if ("DRIVER".equals(user.getRoleCode())
                && !"CARRIER_APPROVED".equals(user.getStatus())) {
            throw new RuntimeException(
                    "기사는 운송사 승인과 트레일러 배정 후 최종 승인할 수 있습니다."
            );
        }
    }

    private void rejectRelatedAccount(UserDTO user) {
        if ("CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(
                    user.getUserId(),
                    "REJECTED"
            );
        }

        if ("DRIVER".equals(user.getRoleCode())) {
            driverMapper.updateApprovalByUserId(
                    user.getUserId(),
                    false,
                    false
            );
        }
    }

    private void validateRegister(RegisterDTO dto) {
        if (dto.getLoginId() == null || dto.getLoginId().isBlank()) {
            throw new RuntimeException("아이디는 필수입니다.");
        }

        if (dto.getPassword() == null || dto.getPassword().length() < 4) {
            throw new RuntimeException("비밀번호는 4자 이상 입력하세요.");
        }

        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            throw new RuntimeException("이름은 필수입니다.");
        }

        if (!List.of(
                "ADMIN",
                "CARRIER",
                "DRIVER"
        ).contains(dto.getRoleCode())) {
            throw new RuntimeException(
                    "역할은 ADMIN, CARRIER, DRIVER 중 하나여야 합니다."
            );
        }
    }

    private void validateCarrierRegister(RegisterDTO dto) {
        if (dto.getCarrierName() == null
                || dto.getCarrierName().isBlank()) {
            throw new RuntimeException("운송사명은 필수입니다.");
        }

        if (dto.getManagerName() == null
                || dto.getManagerName().isBlank()) {
            throw new RuntimeException("운송사 담당자명은 필수입니다.");
        }

        if (dto.getCarrierContact() == null
                || dto.getCarrierContact().isBlank()) {
            throw new RuntimeException("운송사 연락처는 필수입니다.");
        }
    }

    private void validateDriverRegister(RegisterDTO dto) {
        if (dto.getDriverName() == null
                || dto.getDriverName().isBlank()) {
            throw new RuntimeException("기사명은 필수입니다.");
        }

        if (dto.getDriverContact() == null
                || dto.getDriverContact().isBlank()) {
            throw new RuntimeException("기사 연락처는 필수입니다.");
        }

        if (dto.getCarrierId() == null) {
            throw new RuntimeException("소속 운송사는 필수입니다.");
        }
    }

    private void validateDriverTractor(RegisterDTO dto) {
        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw new RuntimeException("트랙터 차량번호는 필수입니다.");
        }

        if (dto.getTonnage() == null || dto.getTonnage().isBlank()) {
            throw new RuntimeException("트랙터 톤수는 필수입니다.");
        }

        if (dto.getTractorNo() == null || dto.getTractorNo().isBlank()) {
            throw new RuntimeException("트랙터 관리번호는 필수입니다.");
        }

        if (dto.getChassisNo() == null || dto.getChassisNo().isBlank()) {
            throw new RuntimeException("샤시번호는 필수입니다.");
        }
    }

}