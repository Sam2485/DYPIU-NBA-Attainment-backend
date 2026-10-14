package com.dypiu.nba.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeCountDto {
    private long unreadChats;
    private long unreadSuggestions;
    private long unreadNotifications;
    private long totalUnread;
}
