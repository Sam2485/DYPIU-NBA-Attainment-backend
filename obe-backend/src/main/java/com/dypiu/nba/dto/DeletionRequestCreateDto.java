package com.dypiu.nba.dto;

import com.dypiu.nba.audit.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionRequestCreateDto {
    @NotNull(message = "resourceType is required")
    private ResourceType resourceType;

    @NotBlank(message = "resourceId is required")
    private String resourceId;

    private String remarks;
}
