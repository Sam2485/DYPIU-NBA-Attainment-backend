import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeBatchAttainmentReportDto.java', 'r') as f:
    text = f.read()

text = re.sub(r'public static class Report3Row \{.*?BigDecimal indirectAttainmentLevel;\n    \}',
r'''public static class Report3PoRow {
        private String poCode;
        private BigDecimal percentageSubstantial;
        private BigDecimal percentageModerate;
        private BigDecimal percentageSlight;
        private BigDecimal weightedScore;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectAttainmentLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report3PsoRow {
        private String psoCode;
        private BigDecimal percentageSubstantial;
        private BigDecimal percentageModerate;
        private BigDecimal percentageSlight;
        private BigDecimal weightedScore;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectAttainmentLevel;
    }''', text, flags=re.DOTALL)


text = re.sub(r'public static class Report4Row \{.*?String observation;\n    \}',
r'''public static class Report4PoRow {
        private String poCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report4PsoRow {
        private String psoCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private String observation;
    }''', text, flags=re.DOTALL)


with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeBatchAttainmentReportDto.java', 'w') as f:
    f.write(text)
