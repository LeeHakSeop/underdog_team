package aaa.user_p.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long userId;

    private String loginId;

    private String password;

    private String userName;

    private String roleCode;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;
}