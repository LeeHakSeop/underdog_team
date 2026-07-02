package aaa.user_p.model;

import lombok.Data;

@Data
public class UserDTO {
    Long userId;
    String loginId;
    String password;
    String userName;
    String roleCode;
    String status;
}
