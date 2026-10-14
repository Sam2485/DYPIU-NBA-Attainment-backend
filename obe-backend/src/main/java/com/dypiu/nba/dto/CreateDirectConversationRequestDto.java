package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDirectConversationRequestDto {

    @NotNull(message = "Peer user ID is required")
    private Long peerUserId;
}
