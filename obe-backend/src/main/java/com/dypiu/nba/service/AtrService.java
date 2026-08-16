package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtrService {

    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final BatchRepository batchRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final ProgrammeRepository programmeRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final ProgrammeTargetRepository programmeTargetRepository;
    private final AttainmentCalculationService attainmentCalculationService;
    private final ObjectMapper objectMapper;

    // --- Legacy and Direct Offering Support ---

    @Transactional(readOnly = true)
    public List<CourseAtr> getCourseAtrs(String courseOfferingOrCourseId) {
        List<CourseAtr> list = courseAtrRepository.findByCourseOfferingId(courseOfferingOrCourseId);
        if (!list.isEmpty()) return list;

        // If courseId was passed, find corresponding course offerings
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseOfferingOrCourseId);
        if (!offerings.isEmpty()) {
            return courseAtrRepository.findByCourseOfferingId(offerings.get(0).getId());
        }
        return Collections.emptyList();
    }

    @Transactional
    public List<CourseAtr> saveCourseAtrs(String courseOfferingId, List<CourseAtr> atrs) {
        atrs.forEach(a -> {
            a.setCourseOfferingId(courseOfferingId);
            if (a.getId() == null || a.getId().isBlank()) {
                a.setId("atr-" + UUID.randomUUID().toString().substring(0, 8));
            }
            if (a.getStatus() == null) {
                a.setStatus(CourseAtrStatus.DRAFT);
            }
        });
        return courseAtrRepository.saveAll(atrs);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getProgrammeAtr(String programmeId) {
        return programmeAtrRepository.findByProgrammeId(programmeId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getProgrammeAtrByBatch(String programmeId, String batchId) {
        if (batchId != null && !batchId.isBlank()) {
            return programmeAtrRepository.findByProgrammeIdAndBatchId(programmeId, batchId);
        }
        return programmeAtrRepository.findByProgrammeId(programmeId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getPreviousBatchProgrammeAtr(String batchId) {
        if (batchId == null || batchId.isBlank()) return Optional.empty();
        Batch currentBatch = batchRepository.findById(batchId).orElse(null);
        if (currentBatch == null || currentBatch.getPreviousBatchId() == null) return Optional.empty();
        return programmeAtrRepository.findByProgrammeIdAndBatchId(currentBatch.getProgrammeId(), currentBatch.getPreviousBatchId());
    }

    @Transactional
    public ProgrammeAtr saveProgrammeAtr(ProgrammeAtr atr) {
        if (atr.getId() == null || atr.getId().isBlank()) {
            atr.setId("patr-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (atr.getStatus() == null) {
            atr.setStatus(ProgrammeAtrStatus.DRAFT);
        }
        return programmeAtrRepository.save(atr);
    }

    // =========================================================================
    //  STANDARD COURSE ATR REPORT (OFFICIAL STRUCTURE)
    // =========================================================================

    @Transactional(readOnly = true)
    public CourseAtrReportDto getCourseAtrReport(String courseOfferingId) {
        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + courseOfferingId));

        Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
        Batch batch = batchRepository.findById(offering.getBatchId()).orElse(null);

        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offering.getId());
        List<CourseAtr> existingAtrs = courseAtrRepository.findByCourseOfferingId(courseOfferingId);
        Map<String, CourseAtr> atrMap = existingAtrs.stream().collect(Collectors.toMap(CourseAtr::getCoCode, a -> a, (a, b) -> a));

        Map<String, Object> calcResult = attainmentCalculationService.calculateCourseCoAttainment(offering.getId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> coAttainments = (List<Map<String, Object>>) calcResult.getOrDefault("coAttainments", Collections.emptyList());
        Map<String, BigDecimal> attainmentScoreMap = new HashMap<>();
        for (Map<String, Object> m : coAttainments) {
            String code = (String) m.get("coCode");
            BigDecimal combined = (BigDecimal) m.get("combinedAttainment");
            if (code != null && combined != null) attainmentScoreMap.put(code, combined);
        }

        List<CourseAtrReportDto.OutcomeRow> rows = new ArrayList<>();
        String atrStatus = existingAtrs.isEmpty() ? "DRAFT" : existingAtrs.get(0).getStatus().name();

        for (CourseOutcome co : cos) {
            String coCode = co.getCode();
            String statement = co.getStatement() != null ? co.getStatement() : "Course Outcome " + coCode;
            BigDecimal target = co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50");

            CourseAtr saved = atrMap.get(coCode);
            BigDecimal actual = saved != null && saved.getActualScore() != null
                    ? saved.getActualScore()
                    : attainmentScoreMap.getOrDefault(coCode, new BigDecimal("2.60"));

            BigDecimal pct = target.compareTo(BigDecimal.ZERO) > 0
                    ? actual.divide(target, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String obs = String.format("%s%% Target %s", pct, actual.compareTo(target) >= 0 ? "Achieved" : "Not Achieved");

            List<String> actions = new ArrayList<>();
            if (saved != null && saved.getActionsJson() != null && !saved.getActionsJson().isBlank()) {
                try {
                    actions = objectMapper.readValue(saved.getActionsJson(), new TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
            }
            if (actions.isEmpty()) {
                actions = List.of(
                        "Action 1: Include supplementary problem solving sets for " + coCode + ".",
                        "Action 2: Conduct diagnostic remedial sessions prior to term-end examinations."
                );
            }

            rows.add(CourseAtrReportDto.OutcomeRow.builder()
                    .outcomeCode(coCode)
                    .outcomeStatement(statement)
                    .targetLevel(target)
                    .attainmentLevel(actual)
                    .achievementPercentage(pct)
                    .observation(obs)
                    .actions(actions)
                    .build());
        }

        return CourseAtrReportDto.builder()
                .reportType("COURSE_ATR")
                .courseAtrId(!existingAtrs.isEmpty() ? existingAtrs.get(0).getId() : "catr-" + offering.getId())
                .courseOffering(CourseAtrReportDto.CourseOfferingSummary.builder()
                        .id(offering.getId())
                        .courseId(offering.getCourseId())
                        .batchId(offering.getBatchId())
                        .semester(offering.getSemester())
                        .build())
                .course(course != null ? CourseAtrReportDto.CourseSummary.builder().id(course.getId()).code(course.getCode()).name(course.getName()).build() : null)
                .batch(batch != null ? CourseAtrReportDto.BatchSummary.builder().id(batch.getId()).name(batch.getName()).build() : null)
                .outcomes(rows)
                .status(atrStatus)
                .build();
    }

    @Transactional
    public CourseAtrReportDto saveCourseAtrReport(CourseAtrReportDto dto) {
        if (dto == null || dto.getCourseOffering() == null || dto.getCourseOffering().getId() == null) {
            throw new IllegalArgumentException("Invalid Course ATR payload: CourseOffering is required.");
        }
        String offeringId = dto.getCourseOffering().getId();
        List<CourseAtr> toSave = new ArrayList<>();

        if (dto.getOutcomes() != null) {
            for (CourseAtrReportDto.OutcomeRow r : dto.getOutcomes()) {
                String actionsJson = "[]";
                try {
                    actionsJson = objectMapper.writeValueAsString(r.getActions() != null ? r.getActions() : Collections.emptyList());
                } catch (Exception ignored) {}

                CourseAtr atr = courseAtrRepository.findByCourseOfferingIdAndCoCode(offeringId, r.getOutcomeCode())
                        .orElseGet(() -> CourseAtr.builder()
                                .id("catr-" + UUID.randomUUID().toString().substring(0, 8))
                                .courseOfferingId(offeringId)
                                .coCode(r.getOutcomeCode())
                                .build());

                atr.setStatement(r.getOutcomeStatement());
                atr.setTargetScore(r.getTargetLevel() != null ? r.getTargetLevel() : new BigDecimal("2.50"));
                atr.setActualScore(r.getAttainmentLevel() != null ? r.getAttainmentLevel() : new BigDecimal("2.60"));
                atr.setPctAchieved(r.getAchievementPercentage() != null ? r.getAchievementPercentage() : new BigDecimal("104.00"));
                atr.setActionsJson(actionsJson);
                atr.setStatus(dto.getStatus() != null ? CourseAtrStatus.valueOf(dto.getStatus()) : CourseAtrStatus.DRAFT);
                atr.setUpdatedAt(ZonedDateTime.now());

                toSave.add(atr);
            }
        }
        courseAtrRepository.saveAll(toSave);
        return getCourseAtrReport(offeringId);
    }

    @Transactional
    public CourseAtr submitCourseAtr(String courseOfferingId, String submittedBy) {
        List<CourseAtr> atrs = courseAtrRepository.findByCourseOfferingId(courseOfferingId);
        if (atrs.isEmpty()) {
            getCourseAtrReport(courseOfferingId); // generates default entries
            atrs = courseAtrRepository.findByCourseOfferingId(courseOfferingId);
        }
        for (CourseAtr a : atrs) {
            a.setStatus(CourseAtrStatus.SUBMITTED_FOR_VERIFICATION);
            a.setSubmittedBy(submittedBy);
            a.setSubmittedAt(ZonedDateTime.now());
        }
        List<CourseAtr> saved = courseAtrRepository.saveAll(atrs);
        return saved.isEmpty() ? null : saved.get(0);
    }

    // =========================================================================
    //  STANDARD PROGRAMME ATR REPORT (OFFICIAL STRUCTURE)
    // =========================================================================

    @Transactional(readOnly = true)
    public ProgrammeAtrReportDto getProgrammeAtrReport(String programmeId, String batchId) {
        Programme prog = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programmeId));
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        ProgrammeAttainmentResultDto attainment = attainmentCalculationService.calculateProgrammeAttainment(programmeId, batchId);
        Optional<ProgrammeAtr> existingAtr = programmeAtrRepository.findByProgrammeIdAndBatchId(programmeId, batchId);

        List<ProgrammeAtrReportDto.OutcomeRow> poRows = new ArrayList<>();
        if (attainment.getOverallAttainment() != null && attainment.getOverallAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : attainment.getOverallAttainment().getPos()) {
                poRows.add(ProgrammeAtrReportDto.OutcomeRow.builder()
                        .outcomeCode(it.getOutcomeCode())
                        .outcomeStatement(it.getOutcomeStatement())
                        .targetLevel(it.getTarget())
                        .attainmentLevel(it.getOverallAttainment())
                        .achievementPercentage(it.getAchievementPercentage())
                        .observation(it.getObservation())
                        .actions(it.getActions())
                        .build());
            }
        }

        List<ProgrammeAtrReportDto.OutcomeRow> psoRows = new ArrayList<>();
        if (attainment.getOverallAttainment() != null && attainment.getOverallAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : attainment.getOverallAttainment().getPsos()) {
                psoRows.add(ProgrammeAtrReportDto.OutcomeRow.builder()
                        .outcomeCode(it.getOutcomeCode())
                        .outcomeStatement(it.getOutcomeStatement())
                        .targetLevel(it.getTarget())
                        .attainmentLevel(it.getOverallAttainment())
                        .achievementPercentage(it.getAchievementPercentage())
                        .observation(it.getObservation())
                        .actions(it.getActions())
                        .build());
            }
        }

        String status = existingAtr.map(a -> a.getStatus() != null ? a.getStatus().name() : "DRAFT").orElse("DRAFT");
        String patrId = existingAtr.map(ProgrammeAtr::getId).orElse("patr-" + programmeId + "-" + batchId);

        return ProgrammeAtrReportDto.builder()
                .reportType("PROGRAMME_ATR")
                .programmeAtrId(patrId)
                .programme(ProgrammeAtrReportDto.ProgrammeSummary.builder().id(prog.getId()).code(prog.getCode()).name(prog.getName()).build())
                .batch(ProgrammeAtrReportDto.BatchSummary.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .startYear(batch.getStartYear() != null ? String.valueOf(batch.getStartYear()) : "")
                        .endYear(batch.getEndYear() != null ? String.valueOf(batch.getEndYear()) : "")
                        .build())

                .poOutcomes(poRows)
                .psoOutcomes(psoRows)
                .status(status)
                .build();
    }

    @Transactional
    public ProgrammeAtrReportDto saveProgrammeAtrReport(ProgrammeAtrReportDto dto) {
        if (dto == null || dto.getProgramme() == null || dto.getBatch() == null) {
            throw new IllegalArgumentException("Invalid Programme ATR payload.");
        }
        String progId = dto.getProgramme().getId();
        String batchId = dto.getBatch().getId();

        ProgrammeAtr atr = programmeAtrRepository.findByProgrammeIdAndBatchId(progId, batchId)
                .orElseGet(() -> ProgrammeAtr.builder()
                        .id("patr-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeId(progId)
                        .batchId(batchId)
                        .build());

        try {
            atr.setObservationsJson(objectMapper.writeValueAsString(dto));
        } catch (Exception ignored) {}

        if (dto.getStatus() != null) {
            try {
                atr.setStatus(ProgrammeAtrStatus.valueOf(dto.getStatus()));
            } catch (Exception ignored) {}
        }
        atr.setUpdatedAt(ZonedDateTime.now());
        programmeAtrRepository.save(atr);

        return getProgrammeAtrReport(progId, batchId);
    }

    @Transactional
    public ProgrammeAtr submitProgrammeAtr(String programmeId, String batchId, String submittedBy) {
        ProgrammeAtr atr = programmeAtrRepository.findByProgrammeIdAndBatchId(programmeId, batchId)
                .orElseGet(() -> ProgrammeAtr.builder()
                        .id("patr-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeId(programmeId)
                        .batchId(batchId)
                        .build());

        atr.setStatus(ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION);
        atr.setSubmittedBy(submittedBy);
        atr.setSubmittedAt(ZonedDateTime.now());
        return programmeAtrRepository.save(atr);
    }

    // =========================================================================
    //  HISTORICAL BATCH COMPARISON
    // =========================================================================

    @Transactional(readOnly = true)
    public BatchComparisonDto getProgrammeBatchComparison(String programmeId, List<String> batchIds) {
        List<Batch> batches = (batchIds != null && !batchIds.isEmpty())
                ? batchRepository.findAllById(batchIds)
                : batchRepository.findByProgrammeId(programmeId);

        List<BatchComparisonDto.BatchAttainmentItem> items = new ArrayList<>();
        for (Batch b : batches) {
            ProgrammeAttainmentResultDto res = attainmentCalculationService.calculateProgrammeAttainment(programmeId, b.getId());
            Optional<ProgrammeAtr> patr = programmeAtrRepository.findByProgrammeIdAndBatchId(programmeId, b.getId());

            Map<String, BigDecimal> poMap = new LinkedHashMap<>();
            Map<String, BigDecimal> psoMap = new LinkedHashMap<>();

            if (res.getOverallAttainment() != null) {
                for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : res.getOverallAttainment().getPos()) {
                    poMap.put(it.getPoCode(), it.getOverallAttainment());
                }
                for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : res.getOverallAttainment().getPsos()) {
                    psoMap.put(it.getPsoCode(), it.getOverallAttainment());
                }
            }

            items.add(BatchComparisonDto.BatchAttainmentItem.builder()
                    .batchId(b.getId())
                    .batchName(b.getName())
                    .programmeAtrStatus(patr.map(a -> a.getStatus().name()).orElse("DRAFT"))
                    .poAttainment(poMap)
                    .psoAttainment(psoMap)
                    .build());
        }

        return BatchComparisonDto.builder()
                .programmeId(programmeId)
                .batches(items)
                .build();
    }
}
