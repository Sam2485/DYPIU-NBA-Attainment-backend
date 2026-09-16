package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuChatResponse {
    private String response;
    private String currency; // "LIVE" or "FINALIZED"
    private EmmuIntent intent;
    private boolean requiresClarification;
    private List<String> clarificationOptions;
    private Map<String, Object> evidenceSummary;
}
