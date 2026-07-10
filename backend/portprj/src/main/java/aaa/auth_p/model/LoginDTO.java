package aaa.auth_p.model;

import lombok.Data;

@Data
public class LoginDTO {
    private String loginId;
    private String password;
    private String roleCode;
}
