package com.examplatform.dto;

import com.examplatform.model.User;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private User.Role role;
}
