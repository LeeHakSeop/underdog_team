package aaa.auth_p.service;

import aaa.auth_p.model.LoginDTO;
import aaa.carrier_p.model.CarrierMapper;
import aaa.driver_p.model.DriverMapper;
import aaa.filter.JwtUtil;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private CarrierMapper carrierMapper;
    @Mock
    private DriverMapper driverMapper;
    @Mock
    private VehicleMapper vehicleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userMapper,
                carrierMapper,
                driverMapper,
                vehicleMapper,
                passwordEncoder,
                jwtUtil
        );
    }

    @Test
    void rejectsUnknownCredentialsAsUnauthorized() {
        LoginDTO dto = login("missing", "wrong", "ADMIN");
        when(userMapper.findByLoginId("missing")).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.login(dto));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.", error.getReason());
    }

    @Test
    void rejectsMismatchedRoleAsUnauthorized() {
        LoginDTO dto = login("carrier", "1234", "ADMIN");
        UserDTO user = new UserDTO();
        user.setLoginId("carrier");
        user.setPassword("1234");
        user.setRoleCode("CARRIER");
        user.setStatus("ACTIVE");
        when(userMapper.findByLoginId("carrier")).thenReturn(user);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.login(dto));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void rejectsPendingAccountAsForbidden() {
        LoginDTO dto = login("carrier", "1234", "CARRIER");
        UserDTO user = new UserDTO();
        user.setLoginId("carrier");
        user.setPassword("1234");
        user.setRoleCode("CARRIER");
        user.setStatus("PENDING");
        when(userMapper.findByLoginId("carrier")).thenReturn(user);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.login(dto));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private LoginDTO login(String loginId, String password, String roleCode) {
        LoginDTO dto = new LoginDTO();
        dto.setLoginId(loginId);
        dto.setPassword(password);
        dto.setRoleCode(roleCode);
        return dto;
    }
}
