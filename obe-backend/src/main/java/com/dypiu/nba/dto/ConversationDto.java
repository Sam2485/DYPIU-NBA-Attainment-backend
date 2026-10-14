package com.dypiu.nba.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {
    private String id;
    private Long userOneId;
    private Long userTwoId;
    private Long peerId;
    private String peerName;
    private String peerEmail;
    private String peerRole;
    private String lastMessagePreview;
    private Long lastMessageSenderId;
    private ZonedDateTime lastMessageAt;
    private long unreadCount;
}
