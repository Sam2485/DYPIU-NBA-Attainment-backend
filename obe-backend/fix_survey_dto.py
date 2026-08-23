import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeSurveyResultDto.java', 'r') as f:
    text = f.read()

text = re.sub(
    r'private List<OutcomeIndirectItem> poIndirectAttainment;\n    private List<OutcomeIndirectItem> psoIndirectAttainment;\n    private String status;\n\n    @Data @Builder @NoArgsConstructor @AllArgsConstructor\n    public static class OutcomeIndirectItem \{\n        private String poCode;\n        private String psoCode;\n        private BigDecimal indirectAttainment;\n    \}',
r'''private List<PoIndirectItem> poIndirectAttainment;
    private List<PsoIndirectItem> psoIndirectAttainment;
    private String status;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PoIndirectItem {
        private String poCode;
        private BigDecimal indirectAttainment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PsoIndirectItem {
        private String psoCode;
        private BigDecimal indirectAttainment;
    }''', text
)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeSurveyResultDto.java', 'w') as f:
    f.write(text)

