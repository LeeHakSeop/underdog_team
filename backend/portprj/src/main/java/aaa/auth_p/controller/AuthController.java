package aaa.auth_p.controller;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.LoginResponseDTO;
import aaa.auth_p.model.RegisterDTO;
import aaa.auth_p.service.AuthService;
import aaa.user_p.model.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody RegisterDTO dto) {
        authService.register(dto);

        return Map.of("message", "회원가입이 완료되었습니다.");
    }

    @PostMapping("/admin-init")
    public Map<String, String> createAdmin() {
        RegisterDTO dto = new RegisterDTO();

        dto.setLoginId("admin");
        dto.setPassword("1234");
        dto.setUserName("시스템 관리자");
        dto.setRoleCode("ADMIN");

        authService.register(dto);

        return Map.of("message", "관리자 계정 생성 완료");
    }

    @GetMapping("/users")
    public List<UserDTO> users() {
        return authService.findUsers();
    }

    @PatchMapping("/users/{userId}/status")
    public UserDTO updateStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body
    ) {
        return authService.updateStatus(userId, body.get("status"));
    }
}
