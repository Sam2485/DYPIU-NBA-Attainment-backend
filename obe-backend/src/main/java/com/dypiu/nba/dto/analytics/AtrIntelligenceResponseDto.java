package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtrIntelligenceResponseDto {

    private int totalAtrRecords;
    private int approvedAtrs;
    private int pendingAtrs;
    private int needsRevisionAtrs;
    private int draftAtrs;
    private int totalGapsInScope;
    private int gapsWithAtr;
    private int gapsWithoutAtr;
    private Map<String, Integer> statusCounts;
    private List<AtrGapDetailDto> gapAtrRecords;
}
