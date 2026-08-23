import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

# Replace report1
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report1Row> report1 = new ArrayList\<\>\(\);\n        if \(calcResult\.getAverageMapping\(\) != null && calcResult\.getAverageMapping\(\)\.getPos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeMappingItem item : calcResult\.getAverageMapping\(\)\.getPos\(\)\) \{\n                report1\.add\(ProgrammeBatchAttainmentReportDto\.Report1Row\.builder\(\)\n                        \.poCode\(item\.getOutcomeCode\(\)\)\n                        \.semesterAverages\(item\.getSemesterAverages\(\)\.stream\(\)\n                                \.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n                                \.collect\(Collectors\.toList\(\)\)\)\n                        \.programmeAverageMapping\(item\.getProgrammeAverage\(\)\)\n                        \.build\(\)\);\n            \}\n        \}\n        if \(calcResult\.getAverageMapping\(\) != null && calcResult\.getAverageMapping\(\)\.getPsos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeMappingItem item : calcResult\.getAverageMapping\(\)\.getPsos\(\)\) \{\n                report1\.add\(ProgrammeBatchAttainmentReportDto\.Report1Row\.builder\(\)\n                        \.psoCode\(item\.getOutcomeCode\(\)\)\n                        \.semesterAverages\(item\.getSemesterAverages\(\)\.stream\(\)\n                                \.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n                                \.collect\(Collectors\.toList\(\)\)\)\n                        \.programmeAverageMapping\(item\.getProgrammeAverage\(\)\)\n                        \.build\(\)\);\n            \}\n        \}',
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

# Replace report2
text = re.sub(
    r'List<ProgrammeBatchAttainmentReportDto\.Report2Row> report2 = new ArrayList\<\>\(\);\n        if \(calcResult\.getAverageDirectAttainment\(\) != null && calcResult\.getAverageDirectAttainment\(\)\.getPos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeDirectItem item : calcResult\.getAverageDirectAttainment\(\)\.getPos\(\)\) \{\n                report2\.add\(ProgrammeBatchAttainmentReportDto\.Report2Row\.builder\(\)\n                        \.poCode\(item\.getOutcomeCode\(\)\)\n                        \.semesterDirectAttainments\(item\.getSemesterAverages\(\)\.stream\(\)\n                                \.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n                                \.collect\(Collectors\.toList\(\)\)\)\n                        \.programmeDirectAttainment\(item\.getProgrammeAverage\(\)\)\n                        \.build\(\)\);\n            \}\n        \}\n        if \(calcResult\.getAverageDirectAttainment\(\) != null && calcResult\.getAverageDirectAttainment\(\)\.getPsos\(\) != null\) \{\n            for \(ProgrammeAttainmentResultDto\.OutcomeDirectItem item : calcResult\.getAverageDirectAttainment\(\)\.getPsos\(\)\) \{\n                report2\.add\(ProgrammeBatchAttainmentReportDto\.Report2Row\.builder\(\)\n                        \.psoCode\(item\.getOutcomeCode\(\)\)\n                        \.semesterDirectAttainments\(item\.getSemesterAverages\(\)\.stream\(\)\n                                \.map\(s -> new ProgrammeBatchAttainmentReportDto\.SemesterContribution\(s\.getSemester\(\), s\.getAverage\(\)\)\)\n                                \.collect\(Collectors\.toList\(\)\)\)\n                        \.programmeDirectAttainment\(item\.getProgrammeAverage\(\)\)\n                        \.build\(\)\);\n            \}\n        \}',
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

# For report3 and report4, I will let python find them since they have logic.
with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

