package aaa.auth_p.controller;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.LoginResponseDTO;
import aaa.auth_p.model.RegisterDTO;
import aaa.auth_p.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Resource
    AuthService service;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginDTO dto) {
        return service.login(dto);
    }

    @PostMapping("/register")
    public int register(@RequestBody RegisterDTO dto) {
        return service.register(dto);
    }
}