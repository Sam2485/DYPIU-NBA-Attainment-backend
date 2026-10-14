package com.dypiu.nba.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private String id;
    private String conversationId;
    private Long senderId;
    private Long recipientId;
    private String content;
    private String iv;
    private Boolean isEncrypted;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}
