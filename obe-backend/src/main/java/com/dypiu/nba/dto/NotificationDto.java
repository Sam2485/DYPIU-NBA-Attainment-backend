package com.dypiu.nba.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String id;
    private Long recipientId;
    private String title;
    private String message;
    private String eventType;
    private String sourceEntityType;
    private String sourceEntityId;
    private String deepLink;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}
