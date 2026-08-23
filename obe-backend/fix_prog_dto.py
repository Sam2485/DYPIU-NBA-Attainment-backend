import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeBatchAttainmentReportDto.java', 'r') as f:
    text = f.read()

# Replace list fields
text = re.sub(r'private List<Report1Row> report1AverageMapping;\n\n    // Report 2',
r'''private List<Report1PoRow> report1AverageMappingPO;
    private List<Report1PsoRow> report1AverageMappingPSO;

    // Report 2''', text)

text = re.sub(r'private List<Report2Row> report2DirectAttainment;\n\n    // Report 3',
r'''private List<Report2PoRow> report2DirectAttainmentPO;
    private List<Report2PsoRow> report2DirectAttainmentPSO;

    // Report 3''', text)

text = re.sub(r'private List<Report3Row> report3IndirectAttainment;\n\n    // Report 4',
r'''private List<Report3PoRow> report3IndirectAttainmentPO;
    private List<Report3PsoRow> report3IndirectAttainmentPSO;

    // Report 4''', text)

text = re.sub(r'private List<Report4Row> report4OverallAttainment;\n\n    private String submittedBy',
r'''private List<Report4PoRow> report4OverallAttainmentPO;
    private List<Report4PsoRow> report4OverallAttainmentPSO;

    private String submittedBy''', text)


# Replace class definitions
text = re.sub(r'public static class Report1Row \{\n        private String poCode;\n        private String psoCode;\n        private List<SemesterContribution> semesterAverages;\n        private BigDecimal programmeAverageMapping;\n    \}',
r'''public static class Report1PoRow {
        private String poCode;
        private List<SemesterContribution> semesterAverages;
        private BigDecimal programmeAverageMapping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report1PsoRow {
        private String psoCode;
        private List<SemesterContribution> semesterAverages;
        private BigDecimal programmeAverageMapping;
    }''', text)

text = re.sub(r'public static class Report2Row \{\n        private String poCode;\n        private String psoCode;\n        private List<SemesterContribution> semesterDirectAttainments;\n        private BigDecimal programmeDirectAttainment;\n    \}',
r'''public static class Report2PoRow {
        private String poCode;
        private List<SemesterContribution> semesterDirectAttainments;
        private BigDecimal programmeDirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report2PsoRow {
        private String psoCode;
        private List<SemesterContribution> semesterDirectAttainments;
        private BigDecimal programmeDirectAttainment;
    }''', text)

text = re.sub(r'public static class Report3Row \{\n        private String poCode;\n        private String psoCode;\n        private BigDecimal percentageSubstantial;\n        private BigDecimal percentageModerate;\n        private BigDecimal percentageSlight;\n        private BigDecimal weightedScore;\n        private BigDecimal indirectPercentage;\n        private BigDecimal indirectAttainmentLevel;\n    \}',
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
    }''', text)

text = re.sub(r'public static class Report4Row \{\n        private String poCode;\n        private String psoCode;\n        private String statement;\n        private BigDecimal targetLevel;\n        private BigDecimal directAttainment;\n        private BigDecimal indirectAttainment;\n        private BigDecimal finalAttainment;\n        private Boolean targetMet;\n        private String observation;\n    \}',
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
    }''', text)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/dto/ProgrammeBatchAttainmentReportDto.java', 'w') as f:
    f.write(text)
