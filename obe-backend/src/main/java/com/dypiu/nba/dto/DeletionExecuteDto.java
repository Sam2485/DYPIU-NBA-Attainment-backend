package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionExecuteDto {
    @NotBlank(message = "Password confirmation is required to execute deletion")
    private String password;
}
