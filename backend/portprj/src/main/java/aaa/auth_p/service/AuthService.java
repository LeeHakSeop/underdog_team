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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserMapper mapper;
    private final CarrierMapper carrierMapper;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserMapper mapper,
            CarrierMapper carrierMapper,
            DriverMapper driverMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.mapper = mapper;
        this.carrierMapper = carrierMapper;
        this.driverMapper = driverMapper;
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

        if ("PENDING".equals(user.getStatus())) {
            throw new RuntimeException("운송사 승인 후 로그인할 수 있습니다.");
        }

        if ("CARRIER_APPROVED".equals(user.getStatus())) {
            throw new RuntimeException("관리자 최종 승인 후 로그인할 수 있습니다.");
        }

        if ("REJECTED".equals(user.getStatus())) {
            throw new RuntimeException("회원가입이 반려되었습니다. 관리자에게 문의하세요.");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("로그인할 수 없는 계정 상태입니다.");
        }

        if ("DRIVER".equals(user.getRoleCode())) {
            DriverDTO driver = driverMapper.findByUserId(user.getUserId());

            if (driver == null || !Boolean.TRUE.equals(driver.getCanEnter())) {
                throw new RuntimeException("차량 승인 후 로그인할 수 있습니다.");
            }
        }

        LoginResponseDTO res = new LoginResponseDTO();
        res.setUserId(user.getUserId());
        res.setLoginId(user.getLoginId());
        res.setUserName(user.getUserName());
        res.setRoleCode(user.getRoleCode());
        res.setStatus(user.getStatus());
        res.setToken(jwtUtil.createToken(user.getLoginId(), user.getRoleCode()));

        return res;
    }

    private boolean checkPassword(String inputPassword, String savedPassword) {
        if (inputPassword == null || savedPassword == null) {
            return false;
        }

        if (savedPassword.startsWith("$2a$") || savedPassword.startsWith("$2b$") || savedPassword.startsWith("$2y$")) {
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

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setStatus("ADMIN".equals(dto.getRoleCode()) ? "ACTIVE" : "PENDING");

        int result = mapper.insertUser(dto);

        if ("CARRIER".equals(dto.getRoleCode())) {
            validateCarrierRegister(dto);
            carrierMapper.insertFromRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            validateDriverRegister(dto);
            driverMapper.insertFromRegister(dto);
        }

        return result;
    }

    public List<UserDTO> findUsers() {
        return mapper.findAll();
    }

    @Transactional
    public UserDTO updateStatus(Long userId, String status) {
        if (!List.of("PENDING", "CARRIER_APPROVED", "ACTIVE", "REJECTED").contains(status)) {
            throw new RuntimeException("상태값이 올바르지 않습니다.");
        }

        UserDTO user = mapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        if ("ACTIVE".equals(status) && "CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(user.getUserId(), "APPROVED");
        }

        if ("REJECTED".equals(status)) {
            if ("CARRIER".equals(user.getRoleCode())) {
                carrierMapper.updateStatusByUserId(user.getUserId(), "REJECTED");
            }

            if ("DRIVER".equals(user.getRoleCode())) {
                driverMapper.updateApprovalByUserId(user.getUserId(), false, false);
            }
        }

        mapper.updateStatus(userId, status);

        UserDTO updatedUser = mapper.findById(userId);
        if (updatedUser != null) {
            updatedUser.setPassword(null);
        }

        return updatedUser;
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

        if (!List.of("ADMIN", "CARRIER", "DRIVER").contains(dto.getRoleCode())) {
            throw new RuntimeException("역할은 ADMIN, CARRIER, DRIVER 중 하나여야 합니다.");
        }
    }

    private void validateCarrierRegister(RegisterDTO dto) {
        if (dto.getCarrierName() == null || dto.getCarrierName().isBlank()) {
            throw new RuntimeException("운송사명은 필수입니다.");
        }
    }

    private void validateDriverRegister(RegisterDTO dto) {
        if (dto.getDriverName() == null || dto.getDriverName().isBlank()) {
            throw new RuntimeException("기사명은 필수입니다.");
        }

        if (dto.getCarrierId() == null) {
            throw new RuntimeException("소속 운송사는 필수입니다.");
        }
    }
}
