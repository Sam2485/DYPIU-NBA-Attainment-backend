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
    private String masterProgrammeId;
    private String programmeBatchId;
    private Map<String, BigDecimal> poTargets;
    private Map<String, BigDecimal> psoTargets;

    public String getProgrammeId() {
        return masterProgrammeId;
    }

    public void setProgrammeId(String programmeId) {
        this.masterProgrammeId = programmeId;
    }

    public String getBatchId() {
        return programmeBatchId;
    }

    public void setBatchId(String batchId) {
        this.programmeBatchId = batchId;
    }

    public static class ProgrammeTargetDtoBuilder {
        public ProgrammeTargetDtoBuilder programmeId(String programmeId) {
            this.masterProgrammeId = programmeId;
            return this;
        }

        public ProgrammeTargetDtoBuilder batchId(String batchId) {
            this.programmeBatchId = batchId;
            return this;
        }
    }
}
