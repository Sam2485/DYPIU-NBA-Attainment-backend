package com.dypiu.nba.reports.template;

import com.dypiu.nba.reports.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BodyDefinition {
    private ReportType reportType;
    @Builder.Default
    private String orientation = "LANDSCAPE"; // "LANDSCAPE" or "PORTRAIT"
    private List<String> sections;
    private String primaryThemeColor;
    private String accentThemeColor;
}
