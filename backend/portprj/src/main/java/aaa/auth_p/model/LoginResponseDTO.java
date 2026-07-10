package aaa.auth_p.model;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private Long userId;
    private String loginId;
    private String userName;
    private String roleCode;
    private String status;
    private String token;
}
