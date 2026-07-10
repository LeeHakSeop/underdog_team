package aaa.config_p.security;

import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId)
            throws UsernameNotFoundException {

        UserDTO user = userMapper.findByLoginId(loginId);

        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        return new CustomUserDetails(user);
    }
}