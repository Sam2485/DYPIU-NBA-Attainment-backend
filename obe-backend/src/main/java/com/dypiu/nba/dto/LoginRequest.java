package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
