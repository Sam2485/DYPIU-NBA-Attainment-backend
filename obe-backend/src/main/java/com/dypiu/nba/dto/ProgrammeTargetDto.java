package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeTargetDto {
    private String programmeId;
    private String batchId;
    private Map<String, BigDecimal> poTargets;
    private Map<String, BigDecimal> psoTargets;

}
