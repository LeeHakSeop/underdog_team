package aaa.auth_p.model;

import lombok.Data;

@Data
public class LoginResponseDTO {

    private Long userId;

    private String loginId;

    private String userName;

    private String roleCode;

    private String status;

    /**
     * origin/main 로그인 응답 호환용
     */
    private String token;

    /**
     * 기존 프론트 로그인 응답 호환용
     */
    private String accessToken;

    private String tokenType = "Bearer";
}