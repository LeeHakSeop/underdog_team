package aaa.user_p.model;

import aaa.auth_p.model.LoginDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT
            user_id AS userId,
            login_id AS loginId,
            password,
            user_name AS userName,
            role_code AS roleCode,
            status
        FROM users
        WHERE login_id = #{loginId}
          AND password = #{password}
          AND status = 'ACTIVE'
    """)
    UserDTO login(LoginDTO dto);
}