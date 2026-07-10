package aaa.user_p.model;

import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT
            user_id AS userId,
            login_id AS loginId,
            password,
            user_name AS userName,
            role_code AS roleCode,
            status,
            created_at AS createdAt
        FROM users
        WHERE login_id = #{loginId}
    """)
    UserDTO findByLoginId(@Param("loginId") String loginId);

    @Select("""
        SELECT
            user_id AS userId,
            login_id AS loginId,
            password,
            user_name AS userName,
            role_code AS roleCode,
            status,
            created_at AS createdAt
        FROM users
        WHERE user_id = #{userId}
    """)
    UserDTO findById(@Param("userId") Long userId);

    @Select("""
        SELECT
            user_id AS userId,
            login_id AS loginId,
            NULL AS password,
            user_name AS userName,
            role_code AS roleCode,
            status,
            created_at AS createdAt
        FROM users
        ORDER BY user_id DESC
    """)
    List<UserDTO> findAll();

    @Select("""
        SELECT COUNT(*)
        FROM users
        WHERE login_id = #{loginId}
    """)
    int countByLoginId(@Param("loginId") String loginId);

    /**
     * 회원가입
     * 생성된 user_id를 RegisterDTO.userId에 자동 저장
     */
    @Insert("""
        INSERT INTO users
        (
            login_id,
            password,
            user_name,
            role_code,
            status,
            created_at
        )
        VALUES
        (
            #{loginId},
            #{password},
            #{userName},
            #{roleCode},
            #{status},
            CURRENT_TIMESTAMP
        )
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "userId",
            keyColumn = "user_id"
    )
    int insertUser(RegisterDTO dto);

    @Update("""
        UPDATE users
        SET status = #{status}
        WHERE user_id = #{userId}
    """)
    int updateStatus(
            @Param("userId") Long userId,
            @Param("status") String status
    );

}
