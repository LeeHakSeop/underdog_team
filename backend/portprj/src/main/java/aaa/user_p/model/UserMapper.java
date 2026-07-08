package aaa.user_p.model;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.Insert;
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

    @Select("""
    SELECT count(*)
    FROM users
    WHERE login_id = #{loginId}
    """)
    int countByLoginId(String loginId);

    @Insert("""
    INSERT INTO users
    (login_id, password, user_name, role_code, status)
    VALUES
    (#{loginId}, #{password}, #{userName}, #{roleCode}, 'ACTIVE')
    """)
    int insertUser(RegisterDTO dto);
}

