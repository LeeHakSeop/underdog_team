package aaa.auth_p.model;

import lombok.Data;

@Data
public class RegisterDTO {
    private String loginId;
    private String password;
    private String userName;
    private String roleCode;
}