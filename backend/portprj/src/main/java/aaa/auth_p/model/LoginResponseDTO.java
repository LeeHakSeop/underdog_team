package aaa.auth_p.model;

import lombok.Data;

@Data
public class LoginResponseDTO {
    Long userId;
    String loginId;
    String userName;
    String roleCode;
    String token;
}