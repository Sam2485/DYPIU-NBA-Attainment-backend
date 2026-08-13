package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String loginSessionId;

    @NotBlank(message = "OTP code is required")
    private String code;
}
