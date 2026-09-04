package com.dypiu.nba.reports.template;

import com.dypiu.nba.reports.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTemplateDto {
    private String id;
    private String templateName;
    private ReportType reportType;
    private Integer templateVersion;
    private Boolean isDefault;
    private String institutionId;
    private HeaderConfig headerConfig;
    private BodyDefinition bodyDefinition;
    private FooterConfig footerConfig;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
