package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    private String newPassword;

    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private String location;
}
