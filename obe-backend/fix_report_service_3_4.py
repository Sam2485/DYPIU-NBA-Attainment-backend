import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

# Replace report3
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report3Row> report3 = new ArrayList\<\>\(\);\n        if \(calcResult\.getAverageIndirectAttainment\(\) != null\) \{\n            for \(Map\.Entry<String, BigDecimal> entry : calcResult\.getAverageIndirectAttainment\(\)\.entrySet\(\)\) \{\n                String code = entry\.getKey\(\);\n                BigDecimal indirect = entry\.getValue\(\);\n                ProgrammeBatchAttainmentReportDto\.Report3Row row = ProgrammeBatchAttainmentReportDto\.Report3Row\.builder\(\)\n                        \.percentageSubstantial\(BigDecimal\.ZERO\)\n                        \.percentageModerate\(BigDecimal\.ZERO\)\n                        \.percentageSlight\(BigDecimal\.ZERO\)\n                        \.weightedScore\(BigDecimal\.ZERO\)\n                        \.indirectPercentage\(indirect\)\n                        \.indirectAttainmentLevel\(indirect\)\n                        \.build\(\);\n                if \(code\.startsWith\("PO"\)\) row\.setPoCode\(code\);\n                else row\.setPsoCode\(code\);\n                report3\.add\(row\);\n            \}\n        \}',
r'''List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = new ArrayList<>();
        if (calcResult.getAverageIndirectAttainment() != null) {
            for (Map.Entry<String, BigDecimal> entry : calcResult.getAverageIndirectAttainment().entrySet()) {
                String code = entry.getKey();
                BigDecimal indirect = entry.getValue();
                
                if (code.startsWith("PO")) {
                    report3PO.add(ProgrammeBatchAttainmentReportDto.Report3PoRow.builder()
                        .poCode(code)
                        .percentageSubstantial(BigDecimal.ZERO)
                        .percentageModerate(BigDecimal.ZERO)
                        .percentageSlight(BigDecimal.ZERO)
                        .weightedScore(BigDecimal.ZERO)
                        .indirectPercentage(indirect)
                        .indirectAttainmentLevel(indirect)
                        .build());
                } else {
                    report3PSO.add(ProgrammeBatchAttainmentReportDto.Report3PsoRow.builder()
                        .psoCode(code)
                        .percentageSubstantial(BigDecimal.ZERO)
                        .percentageModerate(BigDecimal.ZERO)
                        .percentageSlight(BigDecimal.ZERO)
                        .weightedScore(BigDecimal.ZERO)
                        .indirectPercentage(indirect)
                        .indirectAttainmentLevel(indirect)
                        .build());
                }
            }
        }''', text
)

# Replace report4
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report4Row> report4 = new ArrayList\<\>\(\);\n        BigDecimal sumOverall = BigDecimal\.ZERO;\n        int countOverall = 0;\n\n        if \(calcResult\.getOverallAttainment\(\) != null && calcResult\.getOverallAttainment\(\)\.getPos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeAttainmentItem item : calcResult\.getOverallAttainment\(\)\.getPos\(\)\) \{\n                report4\.add\(ProgrammeBatchAttainmentReportDto\.Report4Row\.builder\(\)\n                        \.poCode\(item\.getOutcomeCode\(\)\)\n                        \.targetLevel\(item\.getTargetLevel\(\)\)\n                        \.directAttainment\(item\.getDirectAttainment\(\)\)\n                        \.indirectAttainment\(item\.getIndirectAttainment\(\)\)\n                        \.finalAttainment\(item\.getFinalAttainment\(\)\)\n                        \.targetMet\(item\.getTargetMet\(\)\)\n                        \.build\(\)\);\n                sumOverall = sumOverall\.add\(item\.getFinalAttainment\(\) != null \? item\.getFinalAttainment\(\) : BigDecimal\.ZERO\);\n                countOverall\+\+;\n            \}\n        \}\n        if \(calcResult\.getOverallAttainment\(\) != null && calcResult\.getOverallAttainment\(\)\.getPsos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeAttainmentItem item : calcResult\.getOverallAttainment\(\)\.getPsos\(\)\) \{\n                report4\.add\(ProgrammeBatchAttainmentReportDto\.Report4Row\.builder\(\)\n                        \.psoCode\(item\.getOutcomeCode\(\)\)\n                        \.targetLevel\(item\.getTargetLevel\(\)\)\n                        \.directAttainment\(item\.getDirectAttainment\(\)\)\n                        \.indirectAttainment\(item\.getIndirectAttainment\(\)\)\n                        \.finalAttainment\(item\.getFinalAttainment\(\)\)\n                        \.targetMet\(item\.getTargetMet\(\)\)\n                        \.build\(\)\);\n                sumOverall = sumOverall\.add\(item\.getFinalAttainment\(\) != null \? item\.getFinalAttainment\(\) : BigDecimal\.ZERO\);\n                countOverall\+\+;\n            \}\n        \}',
r'''List<ProgrammeBatchAttainmentReportDto.Report4PoRow> report4PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> report4PSO = new ArrayList<>();
        BigDecimal sumOverall = BigDecimal.ZERO;
        int countOverall = 0;

        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPos()) {
                report4PO.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                        .poCode(item.getOutcomeCode())
                        .targetLevel(item.getTargetLevel())
                        .directAttainment(item.getDirectAttainment())
                        .indirectAttainment(item.getIndirectAttainment())
                        .finalAttainment(item.getFinalAttainment())
                        .targetMet(item.getTargetMet())
                        .build());
                sumOverall = sumOverall.add(item.getFinalAttainment() != null ? item.getFinalAttainment() : BigDecimal.ZERO);
                countOverall++;
            }
        }
        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPsos()) {
                report4PSO.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                        .psoCode(item.getOutcomeCode())
                        .targetLevel(item.getTargetLevel())
                        .directAttainment(item.getDirectAttainment())
                        .indirectAttainment(item.getIndirectAttainment())
                        .finalAttainment(item.getFinalAttainment())
                        .targetMet(item.getTargetMet())
                        .build());
                sumOverall = sumOverall.add(item.getFinalAttainment() != null ? item.getFinalAttainment() : BigDecimal.ZERO);
                countOverall++;
            }
        }''', text
)

# Apply builder
text = re.sub(
    r'\.report1AverageMapping\(report1\)\n                \.report2DirectAttainment\(report2\)\n                \.report3IndirectAttainment\(report3\)\n                \.report4OverallAttainment\(report4\)',
r'''.report1AverageMappingPO(report1PO).report1AverageMappingPSO(report1PSO)
                .report2DirectAttainmentPO(report2PO).report2DirectAttainmentPSO(report2PSO)
                .report3IndirectAttainmentPO(report3PO).report3IndirectAttainmentPSO(report3PSO)
                .report4OverallAttainmentPO(report4PO).report4OverallAttainmentPSO(report4PSO)''', text
)

# Serialization
text = re.sub(
    r'report\.setAverageMappingReportJson\(toJson\(report1\)\);\n        report\.setDirectAttainmentReportJson\(toJson\(report2\)\);\n        report\.setIndirectAttainmentReportJson\(toJson\(report3\)\);\n        report\.setOverallAttainmentReportJson\(toJson\(report4\)\);',
r'''report.setAverageMappingReportJson(toJson(java.util.Map.of("po", report1PO, "pso", report1PSO)));
        report.setDirectAttainmentReportJson(toJson(java.util.Map.of("po", report2PO, "pso", report2PSO)));
        report.setIndirectAttainmentReportJson(toJson(java.util.Map.of("po", report3PO, "pso", report3PSO)));
        report.setOverallAttainmentReportJson(toJson(java.util.Map.of("po", report4PO, "pso", report4PSO)));''', text
)

# Deserialization mapToDto
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report1Row> report1 = fromJson\(report\.getAverageMappingReportJson\(\), new TypeReference<>\(\) \{\}\);\n        List<ProgrammeBatchAttainmentReportDto\.Report2Row> report2 = fromJson\(report\.getDirectAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);\n        List<ProgrammeBatchAttainmentReportDto\.Report3Row> report3 = fromJson\(report\.getIndirectAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);\n        List<ProgrammeBatchAttainmentReportDto\.Report4Row> report4 = fromJson\(report\.getOverallAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);',
r'''java.util.Map<String, Object> r1Map = fromJson(report.getAverageMappingReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report1PoRow> report1PO = r1Map == null ? null : objectMapper.convertValue(r1Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report1PsoRow> report1PSO = r1Map == null ? null : objectMapper.convertValue(r1Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r2Map = fromJson(report.getDirectAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report2PoRow> report2PO = r2Map == null ? null : objectMapper.convertValue(r2Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report2PsoRow> report2PSO = r2Map == null ? null : objectMapper.convertValue(r2Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r3Map = fromJson(report.getIndirectAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = r3Map == null ? null : objectMapper.convertValue(r3Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = r3Map == null ? null : objectMapper.convertValue(r3Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r4Map = fromJson(report.getOverallAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> report4PO = r4Map == null ? null : objectMapper.convertValue(r4Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> report4PSO = r4Map == null ? null : objectMapper.convertValue(r4Map.get("pso"), new TypeReference<>() {});''', text
)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

