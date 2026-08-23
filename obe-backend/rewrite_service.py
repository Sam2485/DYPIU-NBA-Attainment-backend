import re

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'r') as f:
    text = f.read()

# I will replace generateAndSaveProgrammeReport body
replacement = r'''@Transactional
    public ProgrammeBatchAttainmentReportDto generateAndSaveProgrammeReport(MasterProgramme prog, ProgrammeBatch batch, ReportStatus status) {
        ProgrammeAttainmentResultDto calcResult = calculationService.calculateProgrammeAttainment(prog.getId(), batch.getId());

        List<ProgrammeBatchAttainmentReportDto.Report1PoRow> report1PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report1PsoRow> report1PSO = new ArrayList<>();
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageMapping()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report1PO.add(ProgrammeBatchAttainmentReportDto.Report1PoRow.builder()
                        .poCode(item.getPoCode() != null ? item.getPoCode() : item.getOutcomeCode())
                        .semesterAverages(sList)
                        .programmeAverageMapping(item.getOverallAverage())
                        .build());
            }
        }
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPsos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageMapping()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report1PSO.add(ProgrammeBatchAttainmentReportDto.Report1PsoRow.builder()
                        .psoCode(item.getPsoCode() != null ? item.getPsoCode() : item.getOutcomeCode())
                        .semesterAverages(sList)
                        .programmeAverageMapping(item.getOverallAverage())
                        .build());
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report2PoRow> report2PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report2PsoRow> report2PSO = new ArrayList<>();
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageAttainment()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report2PO.add(ProgrammeBatchAttainmentReportDto.Report2PoRow.builder()
                        .poCode(item.getPoCode() != null ? item.getPoCode() : item.getOutcomeCode())
                        .semesterDirectAttainments(sList)
                        .programmeDirectAttainment(item.getOverallAverage())
                        .build());
            }
        }
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPsos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageAttainment()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report2PSO.add(ProgrammeBatchAttainmentReportDto.Report2PsoRow.builder()
                        .psoCode(item.getPsoCode() != null ? item.getPsoCode() : item.getOutcomeCode())
                        .semesterDirectAttainments(sList)
                        .programmeDirectAttainment(item.getOverallAverage())
                        .build());
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = new ArrayList<>();
        if (calcResult.getAverageIndirectAttainment() != null) {
            for (Map.Entry<String, BigDecimal> entry : calcResult.getAverageIndirectAttainment().entrySet()) {
                String code = entry.getKey();
                BigDecimal indirect = entry.getValue();
                if (code.startsWith("PO")) {
                    report3PO.add(ProgrammeBatchAttainmentReportDto.Report3PoRow.builder()
                        .poCode(code).percentageSubstantial(new BigDecimal("100.00")).percentageModerate(BigDecimal.ZERO).percentageSlight(BigDecimal.ZERO).weightedScore(indirect).indirectPercentage(new BigDecimal("100.00")).indirectAttainmentLevel(indirect).build());
                } else {
                    report3PSO.add(ProgrammeBatchAttainmentReportDto.Report3PsoRow.builder()
                        .psoCode(code).percentageSubstantial(new BigDecimal("100.00")).percentageModerate(BigDecimal.ZERO).percentageSlight(BigDecimal.ZERO).weightedScore(indirect).indirectPercentage(new BigDecimal("100.00")).indirectAttainmentLevel(indirect).build());
                }
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> report4PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> report4PSO = new ArrayList<>();
        BigDecimal sumOverall = BigDecimal.ZERO;
        int countOverall = 0;

        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPos()) {
                String code = item.getPoCode() != null ? item.getPoCode() : item.getOutcomeCode();
                BigDecimal target = item.getTarget() != null ? item.getTarget() : new BigDecimal("2.50");
                BigDecimal finalVal = item.getOverallAttainment();
                boolean targetMet = finalVal != null && finalVal.compareTo(target) >= 0;
                String obs = item.getObservation() != null ? item.getObservation() : (targetMet ? "PO target attained (" + finalVal + " >= " + target + ")" : "PO target not attained (" + finalVal + " < " + target + ")");

                report4PO.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                        .poCode(code).statement(item.getOutcomeStatement() != null ? item.getOutcomeStatement() : "Programme Outcome " + code)
                        .targetLevel(target).directAttainment(item.getDirectAttainment()).indirectAttainment(item.getIndirectAttainment()).finalAttainment(finalVal).targetMet(targetMet).observation(obs).build());
                if (finalVal != null) { sumOverall = sumOverall.add(finalVal); countOverall++; }
            }
        }
        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPsos()) {
                String code = item.getPsoCode() != null ? item.getPsoCode() : item.getOutcomeCode();
                BigDecimal target = item.getTarget() != null ? item.getTarget() : new BigDecimal("2.50");
                BigDecimal finalVal = item.getOverallAttainment();
                boolean targetMet = finalVal != null && finalVal.compareTo(target) >= 0;
                String obs = item.getObservation() != null ? item.getObservation() : (targetMet ? "PSO target attained (" + finalVal + " >= " + target + ")" : "PSO target not attained (" + finalVal + " < " + target + ")");

                report4PSO.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                        .psoCode(code).statement(item.getOutcomeStatement() != null ? item.getOutcomeStatement() : "Programme Specific Outcome " + code)
                        .targetLevel(target).directAttainment(item.getDirectAttainment()).indirectAttainment(item.getIndirectAttainment()).finalAttainment(finalVal).targetMet(targetMet).observation(obs).build());
                if (finalVal != null) { sumOverall = sumOverall.add(finalVal); countOverall++; }
            }
        }

        BigDecimal overall = countOverall > 0 ? sumOverall.divide(BigDecimal.valueOf(countOverall), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId())
                .orElse(ProgrammeBatchAttainmentReport.builder()
                        .id("pbar-" + UUID.randomUUID().toString().substring(0, 10))
                        .programmeBatchId(batch.getId())
                        .build());

        report.setStatus(status);
        report.setOverallProgrammeAttainment(overall);
        report.setAverageMappingReportJson(toJson(java.util.Map.of("po", report1PO, "pso", report1PSO)));
        report.setDirectAttainmentReportJson(toJson(java.util.Map.of("po", report2PO, "pso", report2PSO)));
        report.setIndirectAttainmentReportJson(toJson(java.util.Map.of("po", report3PO, "pso", report3PSO)));
        report.setOverallAttainmentReportJson(toJson(java.util.Map.of("po", report4PO, "pso", report4PSO)));
        report.setUpdatedAt(ZonedDateTime.now());

        programmeBatchAttainmentReportRepository.save(report);

        return ProgrammeBatchAttainmentReportDto.builder()
                .id(report.getId())
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .masterProgrammeId(prog.getId())
                .programmeName(prog.getName())
                .programmeCode(prog.getCode())
                .status(report.getStatus())
                .overallProgrammeAttainment(overall)
                .report1AverageMappingPO(report1PO).report1AverageMappingPSO(report1PSO)
                .report2DirectAttainmentPO(report2PO).report2DirectAttainmentPSO(report2PSO)
                .report3IndirectAttainmentPO(report3PO).report3IndirectAttainmentPSO(report3PSO)
                .report4OverallAttainmentPO(report4PO).report4OverallAttainmentPSO(report4PSO)
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .build();
    }'''

text = re.sub(r'@Transactional\n\s*public ProgrammeBatchAttainmentReportDto generateAndSaveProgrammeReport\(MasterProgramme prog, ProgrammeBatch batch, ReportStatus status\).*?\n\s*\}\n\s*private ProgrammeBatchAttainmentReportDto mapToDto\(ProgrammeBatchAttainmentReport report, MasterProgramme prog, ProgrammeBatch batch\)', 
              replacement + '\n\n    private ProgrammeBatchAttainmentReportDto mapToDto(ProgrammeBatchAttainmentReport report, MasterProgramme prog, ProgrammeBatch batch)', text, flags=re.DOTALL)

with open('/Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/AttainmentReportService.java', 'w') as f:
    f.write(text)

