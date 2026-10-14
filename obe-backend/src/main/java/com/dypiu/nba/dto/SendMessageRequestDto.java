package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequestDto {

    @NotBlank(message = "Content cannot be blank")
    private String content;

    private String iv;

    @Builder.Default
    private Boolean isEncrypted = true;
}
