package com.dypiu.nba.reports.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FooterConfig {
    @Builder.Default
    private String standardFooterText = "DYPIU NBA Attainment System · Authoritative Academic Record";
    @Builder.Default
    private Boolean showPageNumbers = true;
    @Builder.Default
    private Boolean showGeneratedTimestamp = true;
    @Builder.Default
    private Boolean showVerificationHash = true;
    private String signatureAssetId;
    private String signatoryName;
    private String signatoryDesignation;
}
