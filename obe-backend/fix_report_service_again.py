import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

text = re.sub(r'List<ProgrammeBatchAttainmentReportDto\.Report1Row> report1 = new ArrayList\<\>\(\);.*?report1\.add\(ProgrammeBatchAttainmentReportDto\.Report1Row\.builder\(\).*?build\(\)\);\n            \}\n        \}',
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
        }''', text, flags=re.DOTALL)


text = re.sub(r'List<ProgrammeBatchAttainmentReportDto\.Report2Row> report2 = new ArrayList\<\>\(\);.*?report2\.add\(ProgrammeBatchAttainmentReportDto\.Report2Row\.builder\(\).*?build\(\)\);\n            \}\n        \}',
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
        }''', text, flags=re.DOTALL)


text = re.sub(r'List<ProgrammeBatchAttainmentReportDto\.Report3Row> report3 = new ArrayList\<\>\(\);.*?report3\.add\(row\);\n            \}\n        \}',
r'''List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = new ArrayList<>();
        if (calcResult.getAverageIndirectAttainment() != null) {
            for (java.util.Map.Entry<String, BigDecimal> entry : calcResult.getAverageIndirectAttainment().entrySet()) {
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
        }''', text, flags=re.DOTALL)


text = re.sub(r'List<ProgrammeBatchAttainmentReportDto\.Report4Row> report4 = new ArrayList\<\>\(\);.*?countOverall\+\+;\n            \}\n        \}',
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
        }''', text, flags=re.DOTALL)


with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

