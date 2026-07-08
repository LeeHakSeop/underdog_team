package aaa.auth_p.service;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.LoginResponseDTO;
import aaa.auth_p.model.RegisterDTO;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Resource
    UserMapper mapper;

    public LoginResponseDTO login(LoginDTO dto) {

        UserDTO user = mapper.login(dto);

        if (user == null) {
            throw new RuntimeException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        LoginResponseDTO res = new LoginResponseDTO();
        res.setUserId(user.getUserId());
        res.setLoginId(user.getLoginId());
        res.setUserName(user.getUserName());
        res.setRoleCode(user.getRoleCode());

        return res;
    }

    public int register(RegisterDTO dto) {

        if (mapper.countByLoginId(dto.getLoginId()) > 0) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }

        return mapper.insertUser(dto);
    }
}
