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

    public String getMasterProgrammeId() {
        return masterProgrammeId;
    }

    public void setMasterProgrammeId(String masterProgrammeId) {
        this.masterProgrammeId = masterProgrammeId;
    }

    public String getProgrammeBatchId() {
        return programmeBatchId;
    }

    public void setProgrammeBatchId(String programmeBatchId) {
        this.programmeBatchId = programmeBatchId;
    }

    public static class ProgrammeTargetDtoBuilder {
        public ProgrammeTargetDtoBuilder masterProgrammeId(String masterProgrammeId) {
            this.masterProgrammeId = masterProgrammeId;
            return this;
        }

        public ProgrammeTargetDtoBuilder programmeBatchId(String programmeBatchId) {
            this.programmeBatchId = programmeBatchId;
            return this;
        }
    }
}
