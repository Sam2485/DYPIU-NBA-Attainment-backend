package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final AttainmentCalculationService attainmentCalculationService;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;
    private final BatchLifecycleService batchLifecycleService;

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService.getCurrentUserScope();
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceSchoolScope(String schoolId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (scope.isDirector() || scope.isHod() || scope.isProgrammeCoordinator()) {
            String requiredSchoolId = scope.getRequiredSchoolId();
            if (schoolId != null && !schoolId.equals(requiredSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned school scope.");
            }
        }
    }

    private void enforceDepartmentScope(String departmentId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (scope.isHod()) {
            String requiredDeptId = scope.getRequiredDepartmentId();
            if (departmentId != null && !departmentId.equals(requiredDeptId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned department scope.");
            }
        }
        if (scope.isProgrammeCoordinator()) {
            if (scope.hasDepartmentScope()) {
                String requiredDeptId = scope.getDepartmentId();
                if (departmentId != null && !departmentId.equals(requiredDeptId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned department scope.");
                }
            }
            return;
        }
        if (scope.isDirector()) {
            if (departmentId != null) {
                Department dept = departmentRepository.findById(departmentId).orElse(null);
                if (dept != null && dept.getSchoolId() != null && !dept.getSchoolId().equals(scope.getRequiredSchoolId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Department is outside your assigned school scope.");
                }
            }
        }
    }

    private void enforceProgrammeScope(String masterProgrammeId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (masterProgrammeId == null || masterProgrammeId.isBlank()) return;

        if (scope.isProgrammeCoordinator()) {
            String requiredProgId = scope.getMasterProgrammeId();
            boolean matchesDirectProg = (requiredProgId != null && masterProgrammeId.equals(requiredProgId));
            boolean matchesBatchProg = false;
            if (!matchesDirectProg && scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(scope.getEmail().trim());
                matchesBatchProg = batches.stream().anyMatch(b -> masterProgrammeId.equals(b.getMasterProgrammeId()));
            }
            if (!matchesDirectProg && !matchesBatchProg) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned programme scope.");
            }
        }

        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + masterProgrammeId));
        if (prog.getDepartmentId() != null) {
            enforceDepartmentScope(prog.getDepartmentId());
            Department dept = departmentRepository.findById(prog.getDepartmentId()).orElse(null);
            if (dept != null && dept.getSchoolId() != null) {
                enforceSchoolScope(dept.getSchoolId());
            }
        }
    }

    private void enforceBatchScope(String programmeBatchId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (programmeBatchId == null || programmeBatchId.isBlank()) return;
        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + programmeBatchId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByProgrammeBatchId(programmeBatchId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()));
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to any Course Offering in this Batch.");
            }
            return;
        }

        enforceProgrammeScope(batch.getMasterProgrammeId());
    }

    private void enforceCourseScope(String masterCourseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (masterCourseId == null || masterCourseId.isBlank()) return;
        MasterCourse course = masterCourseRepository.findById(masterCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + masterCourseId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(masterCourseId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()));
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course.");
            }
            return;
        }

        enforceProgrammeScope(course.getMasterProgrammeId());
    }

    private void enforceOfferingScope(String offeringId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (offeringId == null || offeringId.isBlank()) return;
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));

        if (scope.isFaculty()) {
            boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()));
            boolean isAssigned = isCoordinator || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
            return;
        }

        if (offering.getProgrammeBatchId() != null) enforceBatchScope(offering.getProgrammeBatchId());
        if (offering.getMasterCourseId() != null) enforceCourseScope(offering.getMasterCourseId());
    }

    private void enforceCourseCoordinatorScope(String offeringId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac() || scope.isDirector() || scope.isHod() || scope.isProgrammeCoordinator()) {
            return;
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));
        boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()));
        if (!isCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned Course Coordinator can perform this action.");
        }
    }

    private void enforceCourseOrOfferingScope(String courseOfferingOrMasterCourseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (courseOfferingOrMasterCourseId == null || courseOfferingOrMasterCourseId.isBlank()) return;
        if (programmeBatchCourseRepository.existsById(courseOfferingOrMasterCourseId)) {
            enforceOfferingScope(courseOfferingOrMasterCourseId);
        } else if (masterCourseRepository.existsById(courseOfferingOrMasterCourseId)) {
            enforceCourseScope(courseOfferingOrMasterCourseId);
        }
    }

    private void enforceOfferingEditability(String offeringId) {
        if (offeringId == null || offeringId.isBlank()) return;
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId).orElse(null);
        if (offering != null && offering.getProgrammeBatchId() != null) {
            batchLifecycleService.enforceBatchEditability(offering.getProgrammeBatchId());
        }
    }

    // --- Course ATR Support ---

    @Transactional(readOnly = true)
    public List<CourseAtr> getCourseAtrs(String courseOfferingOrMasterCourseId) {
        System.out.println("[AtrService] getCourseAtrs called | courseOfferingOrMasterCourseId: " + courseOfferingOrMasterCourseId);
        if (courseOfferingOrMasterCourseId != null && !courseOfferingOrMasterCourseId.isBlank()) {
            enforceCourseOrOfferingScope(courseOfferingOrMasterCourseId);
        }
        List<CourseAtr> list = courseAtrRepository.findByProgrammeBatchCourseId(courseOfferingOrMasterCourseId);
        if (!list.isEmpty()) return list;

        // If masterCourseId was passed, find corresponding course offerings
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseOfferingOrMasterCourseId);
        if (!offerings.isEmpty()) {
            return courseAtrRepository.findByProgrammeBatchCourseId(offerings.get(0).getId());
        }
        return Collections.emptyList();
    }

    @Transactional
    public List<CourseAtr> saveCourseAtrs(String programmeBatchCourseId, List<CourseAtr> atrs) {
        System.out.println("[AtrService] saveCourseAtrs called | programmeBatchCourseId: " + programmeBatchCourseId + " | count: " + (atrs != null ? atrs.size() : 0));
        String targetOfferingId = resolveOfferingId(programmeBatchCourseId);
        if (targetOfferingId != null && !targetOfferingId.isBlank()) {
            enforceCourseOrOfferingScope(targetOfferingId);
            enforceOfferingEditability(targetOfferingId);
            if (approvalService != null && approvalService.isCourseAtrApproved(targetOfferingId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Course ATR. A revision must be requested first.");
            }
        }
        List<CourseAtr> existing = courseAtrRepository.findByProgrammeBatchCourseId(targetOfferingId);
        Map<String, CourseAtr> existingByCode = existing.stream()
                .filter(e -> e.getCoCode() != null)
                .collect(Collectors.toMap(e -> e.getCoCode().toUpperCase(), e -> e, (a, b) -> a));

        List<CourseAtr> toSave = new ArrayList<>();
        if (atrs != null) {
            for (CourseAtr a : atrs) {
                String codeKey = a.getCoCode() != null ? a.getCoCode().toUpperCase() : "";
                CourseAtr target = existingByCode.get(codeKey);
                if (target == null) {
                    target = a;
                    target.setProgrammeBatchCourseId(targetOfferingId);
                    if (target.getId() == null || target.getId().isBlank()) {
                        target.setId("atr-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                } else {
                    target.setStatement(a.getStatement());
                    target.setTitle(a.getTitle());
                    target.setTargetScore(a.getTargetScore());
                    target.setActualScore(a.getActualScore());
                    target.setPctAchieved(a.getPctAchieved() != null ? a.getPctAchieved() : BigDecimal.ZERO);
                    target.setActionsJson(a.getActionsJson());
                }
                if (target.getStatus() == null) {
                    target.setStatus(CourseAtrStatus.DRAFT);
                }
                target.setUpdatedAt(ZonedDateTime.now());
                toSave.add(target);
            }
        }
        return courseAtrRepository.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getProgrammeAtr(String programmeBatchId) {
        System.out.println("[AtrService] getProgrammeAtr called | programmeBatchId: " + programmeBatchId);
        if (programmeBatchId != null && !programmeBatchId.isBlank()) {
            enforceBatchScope(programmeBatchId);
        }
        return programmeAtrRepository.findByProgrammeBatchId(programmeBatchId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getProgrammeAtrByBatch(String masterProgrammeId, String programmeBatchId) {
        System.out.println("[AtrService] getProgrammeAtrByBatch called | masterProgrammeId: " + masterProgrammeId + " | programmeBatchId: " + programmeBatchId);
        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            enforceProgrammeScope(masterProgrammeId);
        }
        if (programmeBatchId != null && !programmeBatchId.isBlank()) {
            enforceBatchScope(programmeBatchId);
            return programmeAtrRepository.findByProgrammeBatchId(programmeBatchId);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<ProgrammeAtr> getPreviousBatchProgrammeAtr(String programmeBatchId) {
        System.out.println("[AtrService] getPreviousBatchProgrammeAtr called | programmeBatchId: " + programmeBatchId);
        if (programmeBatchId == null || programmeBatchId.isBlank()) return Optional.empty();
        enforceBatchScope(programmeBatchId);
        
        ProgrammeBatch currentBatch = programmeBatchRepository.findById(programmeBatchId).orElse(null);
        if (currentBatch == null || currentBatch.getStartYear() == null || currentBatch.getEndYear() == null) {
            return Optional.empty();
        }
        
        int prevStartYear = currentBatch.getStartYear() - 1;
        int prevEndYear = currentBatch.getEndYear() - 1;
        
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(currentBatch.getMasterProgrammeId());
        ProgrammeBatch previousBatch = batches.stream()
            .filter(b -> b.getStartYear() != null && b.getEndYear() != null 
                    && b.getStartYear() == prevStartYear 
                    && b.getEndYear() == prevEndYear)
            .findFirst()
            .orElse(null);
            
        if (previousBatch == null) return Optional.empty();
        
        return programmeAtrRepository.findByProgrammeBatchId(previousBatch.getId());
    }

    @Transactional(readOnly = true)
    public ProgrammeAtrReportDto getPreviousYearProgrammeAtrReport(String programmeBatchId) {
        System.out.println("[AtrService] getPreviousYearProgrammeAtrReport called | programmeBatchId: " + programmeBatchId);
        if (programmeBatchId == null || programmeBatchId.isBlank()) return null;
        enforceBatchScope(programmeBatchId);
        ProgrammeBatch currentBatch = programmeBatchRepository.findById(programmeBatchId).orElse(null);
        if (currentBatch == null || currentBatch.getStartYear() == null || currentBatch.getEndYear() == null) return null;
        
        int targetStartYear = currentBatch.getStartYear() - 1;
        int targetEndYear = currentBatch.getEndYear() - 1;
        
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdOrderByStartYearDesc(currentBatch.getMasterProgrammeId());
        ProgrammeBatch targetBatch = batches.stream()
            .filter(b -> b.getStartYear() != null && b.getEndYear() != null 
                    && b.getStartYear() == targetStartYear 
                    && b.getEndYear() == targetEndYear)
            .findFirst()
            .orElse(null);
            
        if (targetBatch == null) return null;
        return getProgrammeAtrReport(currentBatch.getMasterProgrammeId(), targetBatch.getId());
    }

    @Transactional
    public ProgrammeAtr saveProgrammeAtr(ProgrammeAtr atr) {
        System.out.println("[AtrService] saveProgrammeAtr called | id: " + (atr != null ? atr.getId() : "null") + " | programmeBatchId: " + (atr != null ? atr.getProgrammeBatchId() : "null"));
        if (atr != null && atr.getProgrammeBatchId() != null) {
            enforceBatchScope(atr.getProgrammeBatchId());
            batchLifecycleService.enforceBatchEditability(atr.getProgrammeBatchId());
            if (approvalService != null && approvalService.isProgrammeAtrApproved(atr.getProgrammeBatchId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Programme ATR. A revision must be requested first.");
            }
            ProgrammeAtr existing = programmeAtrRepository.findByProgrammeBatchId(atr.getProgrammeBatchId()).orElse(null);
            if (existing != null) {
                atr.setId(existing.getId());
                if (atr.getStatus() == null) atr.setStatus(existing.getStatus());
                if (atr.getSubmittedBy() == null) atr.setSubmittedBy(existing.getSubmittedBy());
                if (atr.getSubmittedAt() == null) atr.setSubmittedAt(existing.getSubmittedAt());
                if (atr.getVerifiedBy() == null) atr.setVerifiedBy(existing.getVerifiedBy());
                if (atr.getVerifiedAt() == null) atr.setVerifiedAt(existing.getVerifiedAt());
                if (atr.getVerificationComments() == null) atr.setVerificationComments(existing.getVerificationComments());
                if (atr.getObservationsJson() == null || atr.getObservationsJson().isBlank()) {
                    atr.setObservationsJson(existing.getObservationsJson());
                }
            }
        }
        if (atr.getId() == null || atr.getId().isBlank()) {
            atr.setId("patr-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (atr.getStatus() == null) {
            atr.setStatus(ProgrammeAtrStatus.DRAFT);
        }
        atr.setUpdatedAt(ZonedDateTime.now());
        return programmeAtrRepository.save(atr);
    }

    public String resolveOfferingId(String offeringOrMasterCourseId) {
        if (offeringOrMasterCourseId == null || offeringOrMasterCourseId.isBlank()) return null;
        if (programmeBatchCourseRepository.existsById(offeringOrMasterCourseId)) {
            return offeringOrMasterCourseId;
        }
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(offeringOrMasterCourseId);
        if (!offerings.isEmpty()) {
            return offerings.get(0).getId();
        }
        return offeringOrMasterCourseId;
    }

    // =========================================================================
    //  STANDARD COURSE ATR REPORT (OFFICIAL STRUCTURE)
    // =========================================================================

    @Transactional(readOnly = true)
    public CourseAtrReportDto getCourseAtrReport(String programmeBatchCourseId) {
        System.out.println("[AtrService] getCourseAtrReport called | programmeBatchCourseId: " + programmeBatchCourseId);
        String targetOfferingId = resolveOfferingId(programmeBatchCourseId);
        if (targetOfferingId != null && !targetOfferingId.isBlank()) {
            enforceOfferingScope(targetOfferingId);
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(targetOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + programmeBatchCourseId));

        return buildCourseAtrReport(offering);
    }

    private CourseAtrReportDto buildCourseAtrReport(ProgrammeBatchCourse offering) {
        MasterCourse course = masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null);
        ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId()).orElse(null);

        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId());
        List<CourseAtr> existingAtrs = courseAtrRepository.findByProgrammeBatchCourseId(offering.getId());
        Map<String, CourseAtr> atrMap = existingAtrs.stream().collect(Collectors.toMap(CourseAtr::getCoCode, a -> a, (a, b) -> a));

        Map<String, BigDecimal> attainmentScoreMap = new HashMap<>();
        if (existingAtrs.isEmpty()) {
            try {
                Map<String, Object> calcResult = attainmentCalculationService.calculateCourseCoAttainment(offering.getId());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> coAttainments = (List<Map<String, Object>>) calcResult.getOrDefault("coAttainments", Collections.emptyList());
                for (Map<String, Object> m : coAttainments) {
                    String code = (String) m.get("coCode");
                    BigDecimal combined = (BigDecimal) m.get("combinedAttainment");
                    if (code != null && combined != null) attainmentScoreMap.put(code, combined);
                }
            } catch (Exception ignored) {}
        }

        List<CourseAtrReportDto.OutcomeRow> rows = new ArrayList<>();
        String atrStatus = existingAtrs.isEmpty() ? "DRAFT" : existingAtrs.get(0).getStatus().name();
        Set<String> processedCodes = new HashSet<>();

        for (CourseOutcome co : cos) {
            String coCode = co.getCode();
            processedCodes.add(coCode.toUpperCase());
            CourseAtr saved = atrMap.get(coCode);

            String statement = (saved != null && saved.getStatement() != null && !saved.getStatement().isBlank())
                    ? saved.getStatement()
                    : (co.getStatement() != null ? co.getStatement() : "Course Outcome " + coCode);

            BigDecimal target = (saved != null && saved.getTargetScore() != null)
                    ? saved.getTargetScore()
                    : (co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50"));

            BigDecimal actual = (saved != null && saved.getActualScore() != null)
                    ? saved.getActualScore()
                    : attainmentScoreMap.getOrDefault(coCode, BigDecimal.ZERO);

            BigDecimal pct = (saved != null && saved.getPctAchieved() != null && saved.getPctAchieved().compareTo(BigDecimal.ZERO) > 0)
                    ? saved.getPctAchieved()
                    : (target.compareTo(BigDecimal.ZERO) > 0
                        ? actual.divide(target, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);

            String obs = String.format("%s%% Target %s", pct, actual.compareTo(target) >= 0 ? "Achieved" : "Not Achieved");

            List<String> actions = new ArrayList<>();
            if (saved != null && saved.getActionsJson() != null && !saved.getActionsJson().isBlank()) {
                try {
                    actions = objectMapper.readValue(saved.getActionsJson(), new TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
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

        // Process any existing ATR records that might not be in CourseOutcome list
        for (CourseAtr saved : existingAtrs) {
            if (saved.getCoCode() != null && !processedCodes.contains(saved.getCoCode().toUpperCase())) {
                String coCode = saved.getCoCode();
                String statement = saved.getStatement() != null ? saved.getStatement() : "Course Outcome " + coCode;
                BigDecimal target = saved.getTargetScore() != null ? saved.getTargetScore() : new BigDecimal("2.50");
                BigDecimal actual = saved.getActualScore() != null ? saved.getActualScore() : BigDecimal.ZERO;
                BigDecimal pct = saved.getPctAchieved() != null ? saved.getPctAchieved() : BigDecimal.ZERO;
                String obs = String.format("%s%% Target %s", pct, actual.compareTo(target) >= 0 ? "Achieved" : "Not Achieved");
                List<String> actions = new ArrayList<>();
                if (saved.getActionsJson() != null && !saved.getActionsJson().isBlank()) {
                    try {
                        actions = objectMapper.readValue(saved.getActionsJson(), new TypeReference<List<String>>() {});
                    } catch (Exception ignored) {}
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
        }

        return CourseAtrReportDto.builder()
                .reportType("COURSE_ATR")
                .courseAtrId(!existingAtrs.isEmpty() ? existingAtrs.get(0).getId() : "catr-" + offering.getId())
                .courseOffering(CourseAtrReportDto.CourseOfferingSummary.builder()
                        .id(offering.getId())
                        .masterCourseId(offering.getMasterCourseId())
                        .programmeBatchId(offering.getProgrammeBatchId())
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
        System.out.println("[AtrService] saveCourseAtrReport called | programmeBatchCourseId: " + (dto != null && dto.getCourseOffering() != null ? dto.getCourseOffering().getId() : "null"));
        if (dto == null || dto.getCourseOffering() == null || dto.getCourseOffering().getId() == null) {
            throw new IllegalArgumentException("Invalid Course ATR payload: CourseOffering is required.");
        }
        String offeringId = resolveOfferingId(dto.getCourseOffering().getId());
        enforceOfferingScope(offeringId);
        enforceOfferingEditability(offeringId);
        if (approvalService != null && approvalService.isCourseAtrApproved(offeringId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Course ATR. A revision must be requested first.");
        }
        List<CourseAtr> toSave = new ArrayList<>();

        if (dto.getOutcomes() != null) {
            for (CourseAtrReportDto.OutcomeRow r : dto.getOutcomes()) {
                String actionsJson = "[]";
                try {
                    actionsJson = objectMapper.writeValueAsString(r.getActions() != null ? r.getActions() : Collections.emptyList());
                } catch (Exception ignored) {}

                CourseAtr atr = courseAtrRepository.findByProgrammeBatchCourseIdAndCoCode(offeringId, r.getOutcomeCode())
                        .orElseGet(() -> CourseAtr.builder()
                                .id("catr-" + UUID.randomUUID().toString().substring(0, 8))
                                .programmeBatchCourseId(offeringId)
                                .coCode(r.getOutcomeCode())
                                .build());

                atr.setStatement(r.getOutcomeStatement());
                atr.setTargetScore(r.getTargetLevel() != null ? r.getTargetLevel() : new BigDecimal("2.50"));
                atr.setActualScore(r.getAttainmentLevel() != null ? r.getAttainmentLevel() : BigDecimal.ZERO);
                atr.setPctAchieved(r.getAchievementPercentage() != null ? r.getAchievementPercentage() : BigDecimal.ZERO);
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
    public CourseAtr submitCourseAtr(String programmeBatchCourseId, String submittedBy) {
        System.out.println("[AtrService] submitCourseAtr called | programmeBatchCourseId: " + programmeBatchCourseId + " | submittedBy: " + submittedBy);
        String targetOfferingId = resolveOfferingId(programmeBatchCourseId);
        if (targetOfferingId != null && !targetOfferingId.isBlank()) {
            enforceOfferingScope(targetOfferingId);
            enforceOfferingEditability(targetOfferingId);
            enforceCourseCoordinatorScope(targetOfferingId);
        }
        List<CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(targetOfferingId);
        if (atrs.isEmpty()) {
            saveCourseAtrReport(getCourseAtrReport(targetOfferingId));
            atrs = courseAtrRepository.findByProgrammeBatchCourseId(targetOfferingId);
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
    public ProgrammeAtrReportDto getProgrammeAtrReport(String masterProgrammeId, String programmeBatchId) {
        System.out.println("[AtrService] getProgrammeAtrReport called | masterProgrammeId: " + masterProgrammeId + " | programmeBatchId: " + programmeBatchId);
        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            enforceProgrammeScope(masterProgrammeId);
        }
        if (programmeBatchId != null && !programmeBatchId.isBlank()) {
            enforceBatchScope(programmeBatchId);
        }
        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + masterProgrammeId));
        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + programmeBatchId));

        Optional<ProgrammeAtr> existingAtr = programmeAtrRepository.findByProgrammeBatchId(programmeBatchId);

        if (existingAtr.isPresent() && existingAtr.get().getObservationsJson() != null && !existingAtr.get().getObservationsJson().isBlank()) {
            try {
                ProgrammeAtrReportDto savedDto = objectMapper.readValue(existingAtr.get().getObservationsJson(), ProgrammeAtrReportDto.class);
                if (savedDto != null && (savedDto.getPoOutcomes() != null || savedDto.getPsoOutcomes() != null)) {
                    savedDto.setStatus(existingAtr.get().getStatus() != null ? existingAtr.get().getStatus().name() : "DRAFT");
                    savedDto.setProgrammeAtrId(existingAtr.get().getId());
                    if (savedDto.getProgramme() == null) {
                        savedDto.setProgramme(ProgrammeAtrReportDto.ProgrammeSummary.builder().id(prog.getId()).code(prog.getCode()).name(prog.getName()).build());
                    }
                    if (savedDto.getBatch() == null) {
                        savedDto.setBatch(ProgrammeAtrReportDto.BatchSummary.builder()
                                .id(batch.getId())
                                .name(batch.getName())
                                .startYear(batch.getStartYear() != null ? String.valueOf(batch.getStartYear()) : "")
                                .endYear(batch.getEndYear() != null ? String.valueOf(batch.getEndYear()) : "")
                                .build());
                    }
                    return savedDto;
                }
            } catch (Exception ignored) {}
        }

        ProgrammeAttainmentResultDto attainment = attainmentCalculationService.calculateProgrammeAttainment(masterProgrammeId, programmeBatchId);

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
                        .actions(it.getActions() != null ? it.getActions() : Collections.emptyList())
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
                        .actions(it.getActions() != null ? it.getActions() : Collections.emptyList())
                        .build());
            }
        }

        String status = existingAtr.map(a -> a.getStatus() != null ? a.getStatus().name() : "DRAFT").orElse("DRAFT");
        String patrId = existingAtr.map(ProgrammeAtr::getId).orElse("patr-" + programmeBatchId);

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
        System.out.println("[AtrService] saveProgrammeAtrReport called | masterProgrammeId: " + (dto != null && dto.getProgramme() != null ? dto.getProgramme().getId() : "null"));
        if (dto == null || dto.getProgramme() == null || dto.getBatch() == null) {
            throw new IllegalArgumentException("Invalid Programme ATR payload.");
        }
        String progId = dto.getProgramme().getId();
        String programmeBatchId = dto.getBatch().getId();
        enforceProgrammeScope(progId);
        enforceBatchScope(programmeBatchId);
        batchLifecycleService.enforceBatchEditability(programmeBatchId);
        if (approvalService != null && approvalService.isProgrammeAtrApproved(programmeBatchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Programme ATR. A revision must be requested first.");
        }

        ProgrammeAtr atr = programmeAtrRepository.findByProgrammeBatchId(programmeBatchId)
                .orElseGet(() -> ProgrammeAtr.builder()
                        .id("patr-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeBatchId(programmeBatchId)
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

        return getProgrammeAtrReport(progId, programmeBatchId);
    }

    @Transactional
    public ProgrammeAtr submitProgrammeAtr(String masterProgrammeId, String programmeBatchId, String submittedBy) {
        System.out.println("[AtrService] submitProgrammeAtr called | masterProgrammeId: " + masterProgrammeId + " | programmeBatchId: " + programmeBatchId + " | submittedBy: " + submittedBy);
        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            enforceProgrammeScope(masterProgrammeId);
        }
        if (programmeBatchId != null && !programmeBatchId.isBlank()) {
            enforceBatchScope(programmeBatchId);
            batchLifecycleService.enforceBatchEditability(programmeBatchId);
        }
        ProgrammeAtr atr = programmeAtrRepository.findByProgrammeBatchId(programmeBatchId)
                .orElseGet(() -> ProgrammeAtr.builder()
                        .id("patr-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeBatchId(programmeBatchId)
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
    public BatchComparisonDto getProgrammeBatchComparison(String masterProgrammeId, List<String> programmeBatchIds) {
        System.out.println("[AtrService] getProgrammeBatchComparison called | masterProgrammeId: " + masterProgrammeId);
        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            enforceProgrammeScope(masterProgrammeId);
        }
        if (programmeBatchIds != null) {
            for (String bId : programmeBatchIds) {
                if (bId != null && !bId.isBlank()) {
                    enforceBatchScope(bId);
                }
            }
        }
        List<ProgrammeBatch> batches = (programmeBatchIds != null && !programmeBatchIds.isEmpty())
                ? programmeBatchRepository.findAllById(programmeBatchIds)
                : programmeBatchRepository.findByMasterProgrammeId(masterProgrammeId);

        List<BatchComparisonDto.BatchAttainmentItem> items = new ArrayList<>();
        for (ProgrammeBatch b : batches) {
            ProgrammeAttainmentResultDto res = attainmentCalculationService.calculateProgrammeAttainment(masterProgrammeId, b.getId());
            Optional<ProgrammeAtr> patr = programmeAtrRepository.findByProgrammeBatchId(b.getId());

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
                    .programmeBatchId(b.getId())
                    .batchName(b.getName())
                    .programmeAtrStatus(patr.map(a -> a.getStatus().name()).orElse("DRAFT"))
                    .poAttainment(poMap)
                    .psoAttainment(psoMap)
                    .build());
        }

        return BatchComparisonDto.builder()
                .masterProgrammeId(masterProgrammeId)
                .batches(items)
                .build();
    }
    @Transactional(readOnly = true)
    public List<CourseAtrReportDto> getHistoricalCourseAtrs(String masterCourseId) {
        System.out.println("[AtrService] getHistoricalCourseAtrs called | masterCourseId: " + masterCourseId);
        enforceCourseScope(masterCourseId);
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(masterCourseId);
        List<CourseAtrReportDto> historicalReports = new ArrayList<>();
        for (ProgrammeBatchCourse offering : offerings) {
            List<CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(offering.getId());
            if (!atrs.isEmpty()) {
                CourseAtrReportDto report = buildCourseAtrReport(offering);
                if (report != null) {
                    historicalReports.add(report);
                }
            }
        }
        return historicalReports;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeAtrReportDto> getHistoricalProgrammeAtrs(String masterProgrammeId) {
        System.out.println("[AtrService] getHistoricalProgrammeAtrs called | masterProgrammeId: " + masterProgrammeId);
        enforceProgrammeScope(masterProgrammeId);
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdOrderByStartYearDesc(masterProgrammeId);
        List<ProgrammeAtrReportDto> historicalReports = new ArrayList<>();
        for (ProgrammeBatch batch : batches) {
            Optional<ProgrammeAtr> atr = programmeAtrRepository.findByProgrammeBatchId(batch.getId());
            if (atr.isPresent()) {
                ProgrammeAtrReportDto report = getProgrammeAtrReport(masterProgrammeId, batch.getId());
                if (report != null) {
                    historicalReports.add(report);
                }
            }
        }
        return historicalReports;
    }

}
