package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeLandscapeResponseDto {

    private List<ProgrammeLandscapeRowDto> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
    private boolean first;
    private boolean last;
}
