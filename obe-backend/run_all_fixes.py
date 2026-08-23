import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

# Fix Table 2
text = re.sub(
    r'List<CourseAttainmentReportDto\.Table2Row> table2 = new ArrayList\<\>\(\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> poAtt = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("poAttainment", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> psoAtt = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("psoAttainment", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> poAvg = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("poAverages", Collections\.emptyMap\(\)\);\n\s*@SuppressWarnings\("unchecked"\)\n\s*Map<String, BigDecimal> psoAvg = \(Map<String, BigDecimal>\) calcResult\.getOrDefault\("psoAverages", Collections\.emptyMap\(\)\);\n\n\s*for \(Map\.Entry<String, BigDecimal> e : poAtt\.entrySet\(\)\) \{\n\s*table2\.add\(CourseAttainmentReportDto\.Table2Row\.builder\(\)\n\s*\.poCode\(e\.getKey\(\)\)\n\s*\.averageMapping\(poAvg\.getOrDefault\(e\.getKey\(\), BigDecimal\.ZERO\)\)\n\s*\.directContribution\(e\.getValue\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*for \(Map\.Entry<String, BigDecimal> e : psoAtt\.entrySet\(\)\) \{\n\s*table2\.add\(CourseAttainmentReportDto\.Table2Row\.builder\(\)\n\s*\.psoCode\(e\.getKey\(\)\)\n\s*\.averageMapping\(psoAvg\.getOrDefault\(e\.getKey\(\), BigDecimal\.ZERO\)\)\n\s*\.directContribution\(e\.getValue\(\)\)\n\s*\.build\(\)\);\n\s*\}',
r'''List<CourseAttainmentReportDto.Table2PoRow> table2PO = new ArrayList<>();
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("poAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("poAverages", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAverages", Collections.emptyMap());

        for (Map.Entry<String, BigDecimal> e : poAtt.entrySet()) {
            table2PO.add(CourseAttainmentReportDto.Table2PoRow.builder()
                    .poCode(e.getKey())
                    .averageMapping(poAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }
        for (Map.Entry<String, BigDecimal> e : psoAtt.entrySet()) {
            table2PSO.add(CourseAttainmentReportDto.Table2PsoRow.builder()
                    .psoCode(e.getKey())
                    .averageMapping(psoAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }''', text
)

# Fix MapToDto table2
text = re.sub(
    r'\.table2Direct\(table2\)',
    r'.table2DirectPO(table2PO).table2DirectPSO(table2PSO)', text
)

text = re.sub(
    r'report\.setTable2DirectJson\(toJson\(table2\)\);',
    r'report.setTable2DirectJson(toJson(java.util.Map.of("po", table2PO, "pso", table2PSO)));', text
)

text = re.sub(
    r'List<CourseAttainmentReportDto\.Table2Row> table2 = fromJson\(report\.getTable2DirectJson\(\), new TypeReference<>\(\) \{\}\);',
r'''java.util.Map<String, Object> t2Map = fromJson(report.getTable2DirectJson(), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PoRow> table2PO = t2Map == null ? null : objectMapper.convertValue(t2Map.get("po"), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = t2Map == null ? null : objectMapper.convertValue(t2Map.get("pso"), new TypeReference<>() {});''', text
)

# Fix Report 1
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report1Row> report1 = new ArrayList\<\>\(\);\n\s*if \(calcResult\.getAverageMapping\(\) != null && calcResult\.getAverageMapping\(\)\.getPos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeMappingItem item : calcResult\.getAverageMapping\(\)\.getPos\(\)\) \{\n\s*report1\.add\(ProgrammeBatchAttainmentReportDto\.Report1Row\.builder\(\)\n\s*\.poCode\(item\.getOutcomeCode\(\)\)\n\s*\.semesterAverages\(item\.getSemesterAverages\(\)\.stream\(\)\n\s*\.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n\s*\.collect\(Collectors\.toList\(\)\)\)\n\s*\.programmeAverageMapping\(item\.getProgrammeAverage\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*\}\n\s*if \(calcResult\.getAverageMapping\(\) != null && calcResult\.getAverageMapping\(\)\.getPsos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeMappingItem item : calcResult\.getAverageMapping\(\)\.getPsos\(\)\) \{\n\s*report1\.add\(ProgrammeBatchAttainmentReportDto\.Report1Row\.builder\(\)\n\s*\.psoCode\(item\.getOutcomeCode\(\)\)\n\s*\.semesterAverages\(item\.getSemesterAverages\(\)\.stream\(\)\n\s*\.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n\s*\.collect\(Collectors\.toList\(\)\)\)\n\s*\.programmeAverageMapping\(item\.getProgrammeAverage\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*\}',
r'''List<ProgrammeBatchAttainmentReportDto.Report1PoRow> report1PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report1PsoRow> report1PSO = new ArrayList<>();
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPos()) {
                report1PO.add(ProgrammeBatchAttainmentReportDto.Report1PoRow.builder()
                        .poCode(item.getOutcomeCode())
                        .semesterAverages(item.getSemesterAverages().stream()
                                .map(s -> new ProgrammeBatchAttainmentReportDto.SemesterContribution(s.getSemester(), s.getAverage()))
                                .collect(Collectors.toList()))
                        .programmeAverageMapping(item.getProgrammeAverage())
                        .build());
            }
        }
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPsos()) {
                report1PSO.add(ProgrammeBatchAttainmentReportDto.Report1PsoRow.builder()
                        .psoCode(item.getOutcomeCode())
                        .semesterAverages(item.getSemesterAverages().stream()
                                .map(s -> new ProgrammeBatchAttainmentReportDto.SemesterContribution(s.getSemester(), s.getAverage()))
                                .collect(Collectors.toList()))
                        .programmeAverageMapping(item.getProgrammeAverage())
                        .build());
            }
        }''', text
)

# Fix Report 2
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report2Row> report2 = new ArrayList\<\>\(\);\n\s*if \(calcResult\.getAverageDirectAttainment\(\) != null && calcResult\.getAverageDirectAttainment\(\)\.getPos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeDirectItem item : calcResult\.getAverageDirectAttainment\(\)\.getPos\(\)\) \{\n\s*report2\.add\(ProgrammeBatchAttainmentReportDto\.Report2Row\.builder\(\)\n\s*\.poCode\(item\.getOutcomeCode\(\)\)\n\s*\.semesterDirectAttainments\(item\.getSemesterAverages\(\)\.stream\(\)\n\s*\.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n\s*\.collect\(Collectors\.toList\(\)\)\)\n\s*\.programmeDirectAttainment\(item\.getProgrammeAverage\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*\}\n\s*if \(calcResult\.getAverageDirectAttainment\(\) != null && calcResult\.getAverageDirectAttainment\(\)\.getPsos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeDirectItem item : calcResult\.getAverageDirectAttainment\(\)\.getPsos\(\)\) \{\n\s*report2\.add\(ProgrammeBatchAttainmentReportDto\.Report2Row\.builder\(\)\n\s*\.psoCode\(item\.getOutcomeCode\(\)\)\n\s*\.semesterDirectAttainments\(item\.getSemesterAverages\(\)\.stream\(\)\n\s*\.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n\s*\.collect\(Collectors\.toList\(\)\)\)\n\s*\.programmeDirectAttainment\(item\.getProgrammeAverage\(\)\)\n\s*\.build\(\)\);\n\s*\}\n\s*\}',
r'''List<ProgrammeBatchAttainmentReportDto.Report2PoRow> report2PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report2PsoRow> report2PSO = new ArrayList<>();
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPos()) {
                report2PO.add(ProgrammeBatchAttainmentReportDto.Report2PoRow.builder()
                        .poCode(item.getOutcomeCode())
                        .semesterDirectAttainments(item.getSemesterAverages().stream()
                                .map(s -> new ProgrammeBatchAttainmentReportDto.SemesterContribution(s.getSemester(), s.getAverage()))
                                .collect(Collectors.toList()))
                        .programmeDirectAttainment(item.getProgrammeAverage())
                        .build());
            }
        }
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPsos()) {
                report2PSO.add(ProgrammeBatchAttainmentReportDto.Report2PsoRow.builder()
                        .psoCode(item.getOutcomeCode())
                        .semesterDirectAttainments(item.getSemesterAverages().stream()
                                .map(s -> new ProgrammeBatchAttainmentReportDto.SemesterContribution(s.getSemester(), s.getAverage()))
                                .collect(Collectors.toList()))
                        .programmeDirectAttainment(item.getProgrammeAverage())
                        .build());
            }
        }''', text
)

# Fix Report 3
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report3Row> report3 = new ArrayList\<\>\(\);\n\s*if \(calcResult\.getAverageIndirectAttainment\(\) != null\) \{\n\s*for \(Map\.Entry<String, BigDecimal> entry : calcResult\.getAverageIndirectAttainment\(\)\.entrySet\(\)\) \{\n\s*String code = entry\.getKey\(\);\n\s*BigDecimal indirect = entry\.getValue\(\);\n\s*ProgrammeBatchAttainmentReportDto\.Report3Row row = ProgrammeBatchAttainmentReportDto\.Report3Row\.builder\(\)\n\s*\.percentageSubstantial\(BigDecimal\.ZERO\)\n\s*\.percentageModerate\(BigDecimal\.ZERO\)\n\s*\.percentageSlight\(BigDecimal\.ZERO\)\n\s*\.weightedScore\(BigDecimal\.ZERO\)\n\s*\.indirectPercentage\(indirect\)\n\s*\.indirectAttainmentLevel\(indirect\)\n\s*\.build\(\);\n\s*if \(code\.startsWith\("PO"\)\) row\.setPoCode\(code\);\n\s*else row\.setPsoCode\(code\);\n\s*report3\.add\(row\);\n\s*\}\n\s*\}',
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

# Fix Report 4
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report4Row> report4 = new ArrayList\<\>\(\);\n\s*BigDecimal sumOverall = BigDecimal\.ZERO;\n\s*int countOverall = 0;\n\n\s*if \(calcResult\.getOverallAttainment\(\) != null && calcResult\.getOverallAttainment\(\)\.getPos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeAttainmentItem item : calcResult\.getOverallAttainment\(\)\.getPos\(\)\) \{\n\s*report4\.add\(ProgrammeBatchAttainmentReportDto\.Report4Row\.builder\(\)\n\s*\.poCode\(item\.getOutcomeCode\(\)\)\n\s*\.targetLevel\(item\.getTargetLevel\(\)\)\n\s*\.directAttainment\(item\.getDirectAttainment\(\)\)\n\s*\.indirectAttainment\(item\.getIndirectAttainment\(\)\)\n\s*\.finalAttainment\(item\.getFinalAttainment\(\)\)\n\s*\.targetMet\(item\.getTargetMet\(\)\)\n\s*\.build\(\)\);\n\s*sumOverall = sumOverall\.add\(item\.getFinalAttainment\(\) != null \? item\.getFinalAttainment\(\) : BigDecimal\.ZERO\);\n\s*countOverall\+\+;\n\s*\}\n\s*\}\n\s*if \(calcResult\.getOverallAttainment\(\) != null && calcResult\.getOverallAttainment\(\)\.getPsos\(\) != null\) \{\n\s*for \(ProgrammeAttainmentResultDto\.OutcomeAttainmentItem item : calcResult\.getOverallAttainment\(\)\.getPsos\(\)\) \{\n\s*report4\.add\(ProgrammeBatchAttainmentReportDto\.Report4Row\.builder\(\)\n\s*\.psoCode\(item\.getOutcomeCode\(\)\)\n\s*\.targetLevel\(item\.getTargetLevel\(\)\)\n\s*\.directAttainment\(item\.getDirectAttainment\(\)\)\n\s*\.indirectAttainment\(item\.getIndirectAttainment\(\)\)\n\s*\.finalAttainment\(item\.getFinalAttainment\(\)\)\n\s*\.targetMet\(item\.getTargetMet\(\)\)\n\s*\.build\(\)\);\n\s*sumOverall = sumOverall\.add\(item\.getFinalAttainment\(\) != null \? item\.getFinalAttainment\(\) : BigDecimal\.ZERO\);\n\s*countOverall\+\+;\n\s*\}\n\s*\}',
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

# Setters
text = re.sub(
    r'report\.setAverageMappingReportJson\(toJson\(report1\)\);\n\s*report\.setDirectAttainmentReportJson\(toJson\(report2\)\);\n\s*report\.setIndirectAttainmentReportJson\(toJson\(report3\)\);\n\s*report\.setOverallAttainmentReportJson\(toJson\(report4\)\);',
r'''report.setAverageMappingReportJson(toJson(java.util.Map.of("po", report1PO, "pso", report1PSO)));
        report.setDirectAttainmentReportJson(toJson(java.util.Map.of("po", report2PO, "pso", report2PSO)));
        report.setIndirectAttainmentReportJson(toJson(java.util.Map.of("po", report3PO, "pso", report3PSO)));
        report.setOverallAttainmentReportJson(toJson(java.util.Map.of("po", report4PO, "pso", report4PSO)));''', text
)

# Builder inside Generate
text = re.sub(
    r'\.report1AverageMapping\(report1\)\n\s*\.report2DirectAttainment\(report2\)\n\s*\.report3IndirectAttainment\(report3\)\n\s*\.report4OverallAttainment\(report4\)',
r'''.report1AverageMappingPO(report1PO).report1AverageMappingPSO(report1PSO)
                .report2DirectAttainmentPO(report2PO).report2DirectAttainmentPSO(report2PSO)
                .report3IndirectAttainmentPO(report3PO).report3IndirectAttainmentPSO(report3PSO)
                .report4OverallAttainmentPO(report4PO).report4OverallAttainmentPSO(report4PSO)''', text
)

# Deserialization in MapToDto
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report1Row> report1 = fromJson\(report\.getAverageMappingReportJson\(\), new TypeReference<>\(\) \{\}\);\n\s*List<ProgrammeBatchAttainmentReportDto\.Report2Row> report2 = fromJson\(report\.getDirectAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);\n\s*List<ProgrammeBatchAttainmentReportDto\.Report3Row> report3 = fromJson\(report\.getIndirectAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);\n\s*List<ProgrammeBatchAttainmentReportDto\.Report4Row> report4 = fromJson\(report\.getOverallAttainmentReportJson\(\), new TypeReference<>\(\) \{\}\);',
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

# Builder inside MapToDto
text = re.sub(
    r'\.report1AverageMapping\(report1 != null \? report1 : Collections\.emptyList\(\)\)\n\s*\.report2DirectAttainment\(report2 != null \? report2 : Collections\.emptyList\(\)\)\n\s*\.report3IndirectAttainment\(report3 != null \? report3 : Collections\.emptyList\(\)\)\n\s*\.report4OverallAttainment\(report4 != null \? report4 : Collections\.emptyList\(\)\)',
r'''.report1AverageMappingPO(report1PO != null ? report1PO : Collections.emptyList()).report1AverageMappingPSO(report1PSO != null ? report1PSO : Collections.emptyList())
                .report2DirectAttainmentPO(report2PO != null ? report2PO : Collections.emptyList()).report2DirectAttainmentPSO(report2PSO != null ? report2PSO : Collections.emptyList())
                .report3IndirectAttainmentPO(report3PO != null ? report3PO : Collections.emptyList()).report3IndirectAttainmentPSO(report3PSO != null ? report3PSO : Collections.emptyList())
                .report4OverallAttainmentPO(report4PO != null ? report4PO : Collections.emptyList()).report4OverallAttainmentPSO(report4PSO != null ? report4PSO : Collections.emptyList())''', text
)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

