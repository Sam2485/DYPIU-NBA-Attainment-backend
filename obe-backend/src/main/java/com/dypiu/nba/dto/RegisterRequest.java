package com.dypiu.nba.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String name;
    private String role; // IQAC, DIRECTOR, HOD, PROGRAMME_COORDINATOR, FACULTY
    private String department;
    private String programme;
}
