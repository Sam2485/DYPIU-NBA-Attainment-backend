package com.dypiu.nba.reports.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderConfig {
    @Builder.Default
    private String institutionName = "D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE";
    @Builder.Default
    private String subHeader = "Sector 29, Nigdi Pradhikaran, Akurdi, Pune, Maharashtra 411044";
    @Builder.Default
    private String accreditationText = "Approved by AICTE | Outcome-Based Education (OBE) NBA Compliance";
    private String headerTitle;
    private String logoAssetId;
    private String secondaryLogoAssetId;
    private String leftLogoAssetId;
    private String rightLogoAssetId;
    @Builder.Default
    private Boolean showLogo = true;

    public String getEffectiveLeftLogoAssetId() {
        return leftLogoAssetId != null && !leftLogoAssetId.isBlank() ? leftLogoAssetId : logoAssetId;
    }

    public String getEffectiveRightLogoAssetId() {
        return rightLogoAssetId != null && !rightLogoAssetId.isBlank() ? rightLogoAssetId : secondaryLogoAssetId;
    }
}
