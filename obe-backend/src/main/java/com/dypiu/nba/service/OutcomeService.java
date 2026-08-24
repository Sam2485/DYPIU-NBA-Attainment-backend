package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.dto.ProgrammeTargetDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.exception.ResourceNotFoundException;
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
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OutcomeService {

    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeOutcomeRepository poRepository;
    private final ProgrammeSpecificOutcomeRepository psoRepository;
    private final PeoOutcomeRepository peoRepository;
    private final CourseOutcomeRepository coRepository;
    private final PoCompetencyRepository poCompetencyRepository;
    private final PsoCompetencyRepository psoCompetencyRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final CourseMappingKeywordRepository courseMappingKeywordRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;
    private final ApprovalService approvalService;
    private final BatchLifecycleService batchLifecycleService;
    private final ObjectMapper objectMapper;

    private static final Comparator<String> NATURAL_CODE_COMPARATOR = (c1, c2) -> {
        if (c1 == null) return -1;
        if (c2 == null) return 1;
        String p1 = c1.replaceAll("\\D+", "");
        String p2 = c2.replaceAll("\\D+", "");
        if (!p1.isEmpty() && !p2.isEmpty()) {
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException ignored) {}
        }
        return c1.compareToIgnoreCase(c2);
    };

    private final Map<String, Map<String, Object>> coursePoKeywordsMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> coursePsoKeywordsMap = new java.util.concurrent.ConcurrentHashMap<>();

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

        private void enforceBatchOrProgrammeScope(String programmeOrBatchId) {
        if (programmeOrBatchId == null || programmeOrBatchId.isBlank()) return;
        if (programmeBatchRepository.existsById(programmeOrBatchId)) {
            enforceBatchScope(programmeOrBatchId);
        } else {
            enforceProgrammeScope(programmeOrBatchId);
        }
    }

    private void enforceProgrammeScope(String programmeId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (programmeId == null || programmeId.isBlank()) return;

        if (scope.isProgrammeCoordinator()) {
            String requiredProgId = scope.getProgrammeId();
            boolean matchesDirectProg = (requiredProgId != null && programmeId.equals(requiredProgId));
            boolean matchesBatchProg = false;
            if (!matchesDirectProg && scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(scope.getEmail().trim());
                matchesBatchProg = batches.stream().anyMatch(b -> programmeId.equals(b.getMasterProgrammeId()));
            }
            if (!matchesDirectProg && !matchesBatchProg) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned programme scope.");
            }
        }

        MasterProgramme prog = masterProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programmeId));
        if (prog.getDepartmentId() != null) {
            enforceDepartmentScope(prog.getDepartmentId());
            Department dept = departmentRepository.findById(prog.getDepartmentId()).orElse(null);
            if (dept != null && dept.getSchoolId() != null) {
                enforceSchoolScope(dept.getSchoolId());
            }
        }
    }

    private void enforceBatchScope(String batchId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (batchId == null || batchId.isBlank()) return;
        ProgrammeBatch batch = programmeBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByProgrammeBatchId(batchId);
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

    private void enforceCourseScope(String courseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (courseId == null || courseId.isBlank()) return;
        MasterCourse course = masterCourseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
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

    private void enforceOfferingEditability(String courseIdOrOfferingId) {
        if (courseIdOrOfferingId == null || courseIdOrOfferingId.isBlank()) return;
        String offeringId = resolveOfferingId(courseIdOrOfferingId);
        if (offeringId != null) {
            ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId).orElse(null);
            if (offering != null && offering.getBatchId() != null) {
                batchLifecycleService.enforceBatchEditability(offering.getBatchId());
            }
        }
    }

    private void enforceCourseOrOfferingScope(String courseIdOrOfferingId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (courseIdOrOfferingId == null || courseIdOrOfferingId.isBlank()) return;
        if (programmeBatchCourseRepository.existsById(courseIdOrOfferingId)) {
            enforceOfferingScope(courseIdOrOfferingId);
        } else if (masterCourseRepository.existsById(courseIdOrOfferingId)) {
            enforceCourseScope(courseIdOrOfferingId);
        }
    }

    private String resolveBatchId(String programmeOrBatchId) {
        if (programmeOrBatchId == null || programmeOrBatchId.isBlank()) return null;
        if (programmeBatchRepository.existsById(programmeOrBatchId)) {
            return programmeOrBatchId;
        }
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(programmeOrBatchId);
        if (!batches.isEmpty()) {
            for (ProgrammeBatch b : batches) {
                if (!poRepository.findByProgrammeBatchId(b.getId()).isEmpty()) {
                    return b.getId();
                }
            }
            return batches.get(0).getId();
        }
        return programmeOrBatchId;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeOutcome> getPOsByProgramme(String programmeOrBatchId) {
        System.out.println("[OutcomeService] getPOsByProgramme called | programmeOrBatchId: " + programmeOrBatchId);
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            return Collections.emptyList();
        }
        List<ProgrammeOutcome> list = poRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId);
        for (ProgrammeOutcome po : list) {
            List<PoCompetency> comps = poCompetencyRepository.findByPoIdOrderByCodeAsc(po.getId());
            comps.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            po.setCompetencies(comps);
        }
        list.sort(Comparator.comparing(ProgrammeOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return list;
    }

    @Transactional
    public List<ProgrammeOutcome> savePOs(String programmeOrBatchId, List<ProgrammeOutcome> pos) {
        System.out.println("[OutcomeService] savePOs called | programmeOrBatchId: " + programmeOrBatchId + " | count: " + (pos != null ? pos.size() : 0));
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            throw new ResourceNotFoundException("Programme Batch not found: " + programmeOrBatchId);
        }
        batchLifecycleService.enforceBatchEditability(batchId);
        
        List<ProgrammeOutcome> existing = poRepository.findByProgrammeBatchId(batchId);
        Map<String, ProgrammeOutcome> existingByCode = existing.stream()
                .collect(Collectors.toMap(
                        p -> p.getCode().toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<ProgrammeOutcome> toSave = new ArrayList<>();
        Map<String, List<PoCompetency>> competenciesMap = new HashMap<>();

        if (pos != null) {
            for (ProgrammeOutcome po : pos) {
                po.setProgrammeBatchId(batchId);

                String key = po.getCode().toLowerCase();
                ProgrammeOutcome targetPo;
                if (existingByCode.containsKey(key)) {
                    targetPo = existingByCode.get(key);
                    targetPo.setStatement(po.getStatement());
                    if (po.getTarget() != null) {
                        targetPo.setTarget(po.getTarget());
                    }
                } else {
                    targetPo = po;
                    if (targetPo.getId() == null || targetPo.getId().isBlank()) {
                        targetPo.setId("po-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                competenciesMap.put(targetPo.getId(), po.getCompetencies());
                processedIds.add(targetPo.getId());
                toSave.add(targetPo);
            }
        }

        // Delete obsolete POs & their competencies
        List<ProgrammeOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            for (ProgrammeOutcome delPo : toDelete) {
                poCompetencyRepository.deleteByPoId(delPo.getId());
            }
            poRepository.deleteAll(toDelete);
        }

        List<ProgrammeOutcome> saved = poRepository.saveAll(toSave);

        for (ProgrammeOutcome po : saved) {
            poCompetencyRepository.deleteByPoId(po.getId());
            poCompetencyRepository.flush();
            List<PoCompetency> rawComps = competenciesMap.get(po.getId());
            List<PoCompetency> compsToSave = new ArrayList<>();
            if (rawComps != null) {
                int cIdx = 1;
                for (PoCompetency c : rawComps) {
                    if (c.getStatement() == null || c.getStatement().isBlank()) continue;
                    String cCode = c.getCode();
                    if (cCode == null || cCode.isBlank()) {
                        cCode = po.getCode() + "." + cIdx;
                    }
                    String cId = c.getId();
                    if (cId == null || cId.isBlank() || cId.startsWith("comp-")) {
                        cId = "pocomp-" + UUID.randomUUID().toString().substring(0, 8);
                    }
                    cIdx++;
                    compsToSave.add(PoCompetency.builder()
                            .id(cId)
                            .poId(po.getId())
                            .code(cCode)
                            .statement(c.getStatement())
                            .build());
                }
            }
            if (!compsToSave.isEmpty()) {
                compsToSave.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                poCompetencyRepository.saveAll(compsToSave);
                poCompetencyRepository.flush();
            }
            po.setCompetencies(compsToSave);
        }

        saved.sort(Comparator.comparing(ProgrammeOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeSpecificOutcome> getPSOsByProgramme(String programmeOrBatchId) {
        System.out.println("[OutcomeService] getPSOsByProgramme called | programmeOrBatchId: " + programmeOrBatchId);
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            return Collections.emptyList();
        }
        List<ProgrammeSpecificOutcome> list = psoRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId);
        for (ProgrammeSpecificOutcome pso : list) {
            List<PsoCompetency> comps = psoCompetencyRepository.findByPsoIdOrderByCodeAsc(pso.getId());
            comps.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            pso.setCompetencies(comps);
        }
        list.sort(Comparator.comparing(ProgrammeSpecificOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return list;
    }

    @Transactional
    public List<ProgrammeSpecificOutcome> savePSOs(String programmeOrBatchId, List<ProgrammeSpecificOutcome> psos) {
        System.out.println("[OutcomeService] savePSOs called | programmeOrBatchId: " + programmeOrBatchId + " | count: " + (psos != null ? psos.size() : 0));
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            throw new ResourceNotFoundException("Programme Batch not found: " + programmeOrBatchId);
        }
        batchLifecycleService.enforceBatchEditability(batchId);

        List<ProgrammeSpecificOutcome> existing = psoRepository.findByProgrammeBatchId(batchId);
        Map<String, ProgrammeSpecificOutcome> existingByCode = existing.stream()
                .collect(Collectors.toMap(
                        p -> p.getCode().toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<ProgrammeSpecificOutcome> toSave = new ArrayList<>();
        Map<String, List<PsoCompetency>> competenciesMap = new HashMap<>();

        if (psos != null) {
            for (ProgrammeSpecificOutcome pso : psos) {
                pso.setProgrammeBatchId(batchId);

                String key = pso.getCode().toLowerCase();
                ProgrammeSpecificOutcome targetPso;
                if (existingByCode.containsKey(key)) {
                    targetPso = existingByCode.get(key);
                    targetPso.setStatement(pso.getStatement());
                    if (pso.getTarget() != null) {
                        targetPso.setTarget(pso.getTarget());
                    }
                } else {
                    targetPso = pso;
                    if (targetPso.getId() == null || targetPso.getId().isBlank()) {
                        targetPso.setId("pso-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                competenciesMap.put(targetPso.getId(), pso.getCompetencies());
                processedIds.add(targetPso.getId());
                toSave.add(targetPso);
            }
        }

        List<ProgrammeSpecificOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            for (ProgrammeSpecificOutcome delPso : toDelete) {
                psoCompetencyRepository.deleteByPsoId(delPso.getId());
            }
            psoRepository.deleteAll(toDelete);
        }

        List<ProgrammeSpecificOutcome> saved = psoRepository.saveAll(toSave);

        for (ProgrammeSpecificOutcome pso : saved) {
            psoCompetencyRepository.deleteByPsoId(pso.getId());
            psoCompetencyRepository.flush();
            List<PsoCompetency> rawComps = competenciesMap.get(pso.getId());
            List<PsoCompetency> compsToSave = new ArrayList<>();
            if (rawComps != null) {
                int cIdx = 1;
                for (PsoCompetency c : rawComps) {
                    if (c.getStatement() == null || c.getStatement().isBlank()) continue;
                    String cCode = c.getCode();
                    if (cCode == null || cCode.isBlank()) {
                        cCode = pso.getCode() + "." + cIdx;
                    }
                    String cId = c.getId();
                    if (cId == null || cId.isBlank() || cId.startsWith("comp-") || cId.startsWith("psocomp-")) {
                        cId = "psocomp-" + UUID.randomUUID().toString().substring(0, 8);
                    }
                    cIdx++;
                    compsToSave.add(PsoCompetency.builder()
                            .id(cId)
                            .psoId(pso.getId())
                            .code(cCode)
                            .statement(c.getStatement())
                            .build());
                }
            }
            if (!compsToSave.isEmpty()) {
                compsToSave.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                psoCompetencyRepository.saveAll(compsToSave);
                psoCompetencyRepository.flush();
            }
            pso.setCompetencies(compsToSave);
        }

        saved.sort(Comparator.comparing(ProgrammeSpecificOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PeoOutcome> getPEOsByProgramme(String programmeOrBatchId) {
        System.out.println("[OutcomeService] getPEOsByProgramme called | programmeOrBatchId: " + programmeOrBatchId);
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            return Collections.emptyList();
        }
        List<PeoOutcome> list = peoRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId);
        list.sort(Comparator.comparing(PeoOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return list;
    }

    @Transactional
    public List<PeoOutcome> savePEOs(String programmeOrBatchId, List<PeoOutcome> peos) {
        System.out.println("[OutcomeService] savePEOs called | programmeOrBatchId: " + programmeOrBatchId + " | count: " + (peos != null ? peos.size() : 0));
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            throw new ResourceNotFoundException("Programme Batch not found: " + programmeOrBatchId);
        }
        batchLifecycleService.enforceBatchEditability(batchId);

        List<PeoOutcome> existing = peoRepository.findByProgrammeBatchId(batchId);
        Map<String, PeoOutcome> existingByCode = existing.stream()
                .collect(Collectors.toMap(
                        p -> p.getCode().toLowerCase(),
                        p -> p,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<PeoOutcome> toSave = new ArrayList<>();

        if (peos != null) {
            for (PeoOutcome peo : peos) {
                peo.setProgrammeBatchId(batchId);

                String key = peo.getCode().toLowerCase();
                PeoOutcome targetPeo;
                if (existingByCode.containsKey(key)) {
                    targetPeo = existingByCode.get(key);
                    targetPeo.setStatement(peo.getStatement());
                } else {
                    targetPeo = peo;
                    if (targetPeo.getId() == null || targetPeo.getId().isBlank()) {
                        targetPeo.setId("peo-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                }

                processedIds.add(targetPeo.getId());
                toSave.add(targetPeo);
            }
        }

        List<PeoOutcome> toDelete = existing.stream()
                .filter(p -> !processedIds.contains(p.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            peoRepository.deleteAll(toDelete);
        }

        List<PeoOutcome> saved = peoRepository.saveAll(toSave);
        saved.sort(Comparator.comparing(PeoOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return saved;
    }

    private String resolveOfferingId(String offeringOrCourseId) {
        if (offeringOrCourseId == null || offeringOrCourseId.isBlank()) return null;
        if (programmeBatchCourseRepository.existsById(offeringOrCourseId)) {
            return offeringOrCourseId;
        }
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(offeringOrCourseId);
        if (!offerings.isEmpty()) {
            return offerings.get(0).getId();
        }
        return offeringOrCourseId;
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getCOsByCourse(String courseIdOrOfferingId) {
        System.out.println("[OutcomeService] getCOsByCourse called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        if (courseIdOrOfferingId != null && !courseIdOrOfferingId.isBlank()) {
            enforceCourseOrOfferingScope(courseIdOrOfferingId);
        }
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        List<CourseOutcome> list = coRepository.findByProgrammeBatchCourseId(targetOfferingId);
        list.sort(Comparator.comparing(CourseOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return list;
    }

    @Transactional
    public List<CourseOutcome> saveCOs(String courseIdOrOfferingId, List<CourseOutcome> cos) {
        System.out.println("[OutcomeService] saveCOs called | courseIdOrOfferingId: " + courseIdOrOfferingId + " | count: " + (cos != null ? cos.size() : 0));
        if (courseIdOrOfferingId != null && !courseIdOrOfferingId.isBlank()) {
            enforceCourseOrOfferingScope(courseIdOrOfferingId);
            enforceOfferingEditability(courseIdOrOfferingId);
        }
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        if (approvalService != null && approvalService.isCoDefinitionApproved(targetOfferingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Course Outcomes. A revision must be requested first.");
        }
        List<CourseOutcome> existing = coRepository.findByProgrammeBatchCourseId(targetOfferingId);
        Map<String, CourseOutcome> existingByCode = existing.stream()
                .collect(Collectors.toMap(
                        c -> c.getCode().toLowerCase(),
                        c -> c,
                        (e1, e2) -> e1
                ));

        Set<String> processedIds = new HashSet<>();
        List<CourseOutcome> toSave = new ArrayList<>();

        if (cos != null) {
            for (CourseOutcome co : cos) {
                co.setProgrammeBatchCourseId(targetOfferingId);

                String key = co.getCode().toLowerCase();
                CourseOutcome targetCo;
                if (existingByCode.containsKey(key)) {
                    targetCo = existingByCode.get(key);
                    targetCo.setStatement(co.getStatement());
                    if (co.getTargetLevel() != null) {
                        targetCo.setTargetLevel(co.getTargetLevel());
                    }
                } else {
                    targetCo = co;
                    if (targetCo.getId() == null || targetCo.getId().isBlank()) {
                        targetCo.setId("co-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                    if (targetCo.getTargetLevel() == null) {
                        targetCo.setTargetLevel(new BigDecimal("2.50"));
                    }
                }

                processedIds.add(targetCo.getId());
                toSave.add(targetCo);
            }
        }

        List<CourseOutcome> toDelete = existing.stream()
                .filter(c -> !processedIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            coRepository.deleteAll(toDelete);
        }

        List<CourseOutcome> saved = coRepository.saveAll(toSave);
        saved.sort(Comparator.comparing(CourseOutcome::getCode, NATURAL_CODE_COMPARATOR));
        return saved;
    }

    // --- Programme Target Benchmark Levels ---
    @Transactional(readOnly = true)
    public ProgrammeTargetDto getProgrammeTargets(String programmeOrBatchId) {
        System.out.println("[OutcomeService] getProgrammeTargets called | programmeOrBatchId: " + programmeOrBatchId);
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        String batchId = resolveBatchId(programmeOrBatchId);
        if (batchId == null || batchId.isBlank()) {
            return ProgrammeTargetDto.builder()
                    .programmeId(programmeOrBatchId)
                    .poTargets(Collections.emptyMap())
                    .psoTargets(Collections.emptyMap())
                    .build();
        }

        List<ProgrammeOutcome> pos = poRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId);
        List<ProgrammeSpecificOutcome> psos = psoRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId);

        Map<String, BigDecimal> poTargets = new LinkedHashMap<>();
        Map<String, BigDecimal> psoTargets = new LinkedHashMap<>();

        for (ProgrammeOutcome po : pos) {
            if (po.getCode() != null && po.getTarget() != null) {
                poTargets.put(po.getCode(), po.getTarget());
            }
        }
        for (ProgrammeSpecificOutcome pso : psos) {
            if (pso.getCode() != null && pso.getTarget() != null) {
                psoTargets.put(pso.getCode(), pso.getTarget());
            }
        }

        return ProgrammeTargetDto.builder()
                .programmeId(programmeOrBatchId)
                .batchId(batchId)
                .poTargets(poTargets)
                .psoTargets(psoTargets)
                .build();
    }

    @Transactional(readOnly = true)
    public ProgrammeTargetDto getBatchProgrammeTargets(String batchId) {
        return getProgrammeTargets(batchId);
    }

    @Transactional
    public ProgrammeTargetDto saveProgrammeTargets(String programmeOrBatchId, ProgrammeTargetDto dto) {
        System.out.println("[OutcomeService] saveProgrammeTargets called | programmeOrBatchId: " + programmeOrBatchId);
        enforceBatchOrProgrammeScope(programmeOrBatchId);
        if (approvalService != null && approvalService.isPoPsoTargetsApproved(programmeOrBatchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Programme PO/PSO targets. A revision must be requested first.");
        }
        String batchId = (dto != null && dto.getBatchId() != null && !dto.getBatchId().isBlank())
                ? dto.getBatchId()
                : resolveBatchId(programmeOrBatchId);

        if (batchId == null || batchId.isBlank()) {
            throw new ResourceNotFoundException("Programme Batch not found: " + programmeOrBatchId);
        }
        batchLifecycleService.enforceBatchEditability(batchId);

        if (dto != null && dto.getPoTargets() != null && !dto.getPoTargets().isEmpty()) {
            List<ProgrammeOutcome> pos = new ArrayList<>(poRepository.findByProgrammeBatchId(batchId));
            Map<String, ProgrammeOutcome> poMap = pos.stream()
                    .filter(p -> p.getCode() != null)
                    .collect(Collectors.toMap(p -> p.getCode().trim().toUpperCase(), p -> p, (a, b) -> a));
            List<ProgrammeOutcome> toSave = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : dto.getPoTargets().entrySet()) {
                if (entry.getKey() == null) continue;
                String rawCode = entry.getKey().trim();
                String code = rawCode.toUpperCase();
                BigDecimal val = entry.getValue();

                ProgrammeOutcome po = poMap.get(code);
                if (po == null && !code.startsWith("PO") && !code.startsWith("PSO")) {
                    po = poMap.get("PO" + code);
                }

                if (po != null) {
                    po.setTarget(val);
                    toSave.add(po);
                } else {
                    String finalCode = rawCode.matches("\\d+") ? "PO" + rawCode : rawCode;
                    ProgrammeOutcome newPo = ProgrammeOutcome.builder()
                            .id("po-" + UUID.randomUUID().toString().substring(0, 8))
                            .programmeBatchId(batchId)
                            .code(finalCode)
                            .statement("Programme Outcome " + finalCode)
                            .target(val)
                            .build();
                    toSave.add(newPo);
                }
            }
            if (!toSave.isEmpty()) {
                poRepository.saveAll(toSave);
            }
        }

        if (dto != null && dto.getPsoTargets() != null && !dto.getPsoTargets().isEmpty()) {
            List<ProgrammeSpecificOutcome> psos = new ArrayList<>(psoRepository.findByProgrammeBatchId(batchId));
            Map<String, ProgrammeSpecificOutcome> psoMap = psos.stream()
                    .filter(p -> p.getCode() != null)
                    .collect(Collectors.toMap(p -> p.getCode().trim().toUpperCase(), p -> p, (a, b) -> a));
            List<ProgrammeSpecificOutcome> toSave = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : dto.getPsoTargets().entrySet()) {
                if (entry.getKey() == null) continue;
                String rawCode = entry.getKey().trim();
                String code = rawCode.toUpperCase();
                BigDecimal val = entry.getValue();

                ProgrammeSpecificOutcome pso = psoMap.get(code);
                if (pso == null && !code.startsWith("PSO")) {
                    pso = psoMap.get("PSO" + code);
                }

                if (pso != null) {
                    pso.setTarget(val);
                    toSave.add(pso);
                } else {
                    String finalCode = rawCode.matches("\\d+") ? "PSO" + rawCode : rawCode;
                    ProgrammeSpecificOutcome newPso = ProgrammeSpecificOutcome.builder()
                            .id("pso-" + UUID.randomUUID().toString().substring(0, 8))
                            .programmeBatchId(batchId)
                            .code(finalCode)
                            .statement("Programme Specific Outcome " + finalCode)
                            .target(val)
                            .build();
                    toSave.add(newPso);
                }
            }
            if (!toSave.isEmpty()) {
                psoRepository.saveAll(toSave);
            }
        }

        return getProgrammeTargets(batchId);
    }

    @Transactional
    public CourseMappingMatrixDto getCourseMappings(String courseIdOrOfferingId) {
        System.out.println("[OutcomeService] getCourseMappings called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        if (courseIdOrOfferingId != null && !courseIdOrOfferingId.isBlank()) {
            enforceCourseOrOfferingScope(courseIdOrOfferingId);
        }
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(targetOfferingId).orElse(null);
        String courseId = offering != null ? offering.getMasterCourseId() : targetOfferingId;
        String batchId = offering != null ? offering.getProgrammeBatchId() : null;

        MasterCourse course = masterCourseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getMasterProgrammeId() : null;

        List<CourseOutcome> cos = getCOsByCourse(targetOfferingId);
        List<ProgrammeOutcome> pos = (batchId != null) ? getPOsByProgramme(batchId) : ((progId != null) ? getPOsByProgramme(progId) : Collections.emptyList());
        List<ProgrammeSpecificOutcome> psos = (batchId != null) ? getPSOsByProgramme(batchId) : ((progId != null) ? getPSOsByProgramme(progId) : Collections.emptyList());

        List<String> coIds = cos.stream().map(CourseOutcome::getId).collect(Collectors.toList());

        List<CoPoMapping> poMappings = coIds.isEmpty() ? Collections.emptyList() : coPoMappingRepository.findByCourseOutcomeIdIn(coIds);
        List<CoPsoMapping> psoMappings = coIds.isEmpty() ? Collections.emptyList() : coPsoMappingRepository.findByCourseOutcomeIdIn(coIds);

        Map<String, Object> poKw = Collections.emptyMap();
        Map<String, Object> psoKw = Collections.emptyMap();

        Optional<CourseMappingKeyword> poKwOpt = courseMappingKeywordRepository.findByProgrammeBatchCourseIdAndKeywordType(targetOfferingId, "PO");
        if (poKwOpt.isPresent()) {
            try {
                poKw = objectMapper.readValue(poKwOpt.get().getKeywordsJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        } else if (coursePoKeywordsMap.containsKey(targetOfferingId)) {
            poKw = coursePoKeywordsMap.get(targetOfferingId);
        }

        Optional<CourseMappingKeyword> psoKwOpt = courseMappingKeywordRepository.findByProgrammeBatchCourseIdAndKeywordType(targetOfferingId, "PSO");
        if (psoKwOpt.isPresent()) {
            try {
                psoKw = objectMapper.readValue(psoKwOpt.get().getKeywordsJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        } else if (coursePsoKeywordsMap.containsKey(targetOfferingId)) {
            psoKw = coursePsoKeywordsMap.get(targetOfferingId);
        }

        Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
        Map<String, List<Integer>> poVals = new LinkedHashMap<>();
        Map<String, List<Integer>> psoVals = new LinkedHashMap<>();

        Map<String, String> coIdToCode = cos.stream().collect(Collectors.toMap(CourseOutcome::getId, CourseOutcome::getCode, (a, b) -> a));

        for (CourseOutcome co : cos) {
            matrix.put(co.getCode(), new LinkedHashMap<>());
        }

        for (CoPoMapping m : poMappings) {
            String coCode = coIdToCode.get(m.getCourseOutcomeId());
            if (coCode != null && matrix.containsKey(coCode)) {
                matrix.get(coCode).put(m.getPoCode(), m.getMappingLevel());
            }
            if (m.getMappingLevel() != null && m.getMappingLevel() > 0) {
                poVals.computeIfAbsent(m.getPoCode(), k -> new ArrayList<>()).add(m.getMappingLevel());
            }
        }

        for (CoPsoMapping m : psoMappings) {
            String coCode = coIdToCode.get(m.getCourseOutcomeId());
            if (coCode != null && matrix.containsKey(coCode)) {
                matrix.get(coCode).put(m.getPsoCode(), m.getMappingLevel());
            }
            if (m.getMappingLevel() != null && m.getMappingLevel() > 0) {
                psoVals.computeIfAbsent(m.getPsoCode(), k -> new ArrayList<>()).add(m.getMappingLevel());
            }
        }

        Map<String, BigDecimal> poAverages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : poVals.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            poAverages.put(e.getKey(), BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        Map<String, BigDecimal> psoAverages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : psoVals.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            psoAverages.put(e.getKey(), BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        return CourseMappingMatrixDto.builder()
                .courseId(courseId)
                .programmeId(progId)
                .cos(cos)
                .pos(pos)
                .psos(psos)
                .poMappings(poMappings)
                .psoMappings(psoMappings)
                .poKeywordsStore(poKw)
                .psoKeywordsStore(psoKw)
                .matrix(matrix)
                .poAverages(poAverages)
                .psoAverages(psoAverages)
                .build();
    }

    @Transactional
    public CourseMappingMatrixDto saveCourseMappings(String courseIdOrOfferingId, CourseMappingMatrixDto dto) {
        System.out.println("[OutcomeService] saveCourseMappings called | courseIdOrOfferingId: " + courseIdOrOfferingId);
        if (courseIdOrOfferingId != null && !courseIdOrOfferingId.isBlank()) {
            enforceCourseOrOfferingScope(courseIdOrOfferingId);
            enforceOfferingEditability(courseIdOrOfferingId);
        }
        String targetOfferingId = resolveOfferingId(courseIdOrOfferingId);
        if (approvalService != null && approvalService.isCoDefinitionApproved(targetOfferingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Course Outcomes / Mappings. A revision must be requested first.");
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(targetOfferingId).orElse(null);
        String courseId = offering != null ? offering.getMasterCourseId() : targetOfferingId;
        String batchId = offering != null ? offering.getProgrammeBatchId() : null;

        MasterCourse course = masterCourseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getMasterProgrammeId() : (dto != null && dto.getProgrammeId() != null ? dto.getProgrammeId() : null);

        Map<String, Object> poKwToReturn = Collections.emptyMap();
        Map<String, Object> psoKwToReturn = Collections.emptyMap();

        if (dto != null && dto.getPoKeywordsStore() != null) {
            poKwToReturn = dto.getPoKeywordsStore();
            coursePoKeywordsMap.put(targetOfferingId, poKwToReturn);
            try {
                String poJson = objectMapper.writeValueAsString(poKwToReturn);
                CourseMappingKeyword entity = courseMappingKeywordRepository.findByProgrammeBatchCourseIdAndKeywordType(targetOfferingId, "PO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-po-" + UUID.randomUUID().toString().substring(0, 8))
                                .programmeBatchCourseId(targetOfferingId)
                                .keywordType("PO")
                                .build());
                entity.setKeywordsJson(poJson);
                courseMappingKeywordRepository.save(entity);
            } catch (Exception ignored) {}
        }

        if (dto != null && dto.getPsoKeywordsStore() != null) {
            psoKwToReturn = dto.getPsoKeywordsStore();
            coursePsoKeywordsMap.put(targetOfferingId, psoKwToReturn);
            try {
                String psoJson = objectMapper.writeValueAsString(psoKwToReturn);
                CourseMappingKeyword entity = courseMappingKeywordRepository.findByProgrammeBatchCourseIdAndKeywordType(targetOfferingId, "PSO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-pso-" + UUID.randomUUID().toString().substring(0, 8))
                                .programmeBatchCourseId(targetOfferingId)
                                .keywordType("PSO")
                                .build());
                entity.setKeywordsJson(psoJson);
                courseMappingKeywordRepository.save(entity);
            } catch (Exception ignored) {}
        }

        List<CourseOutcome> cos = getCOsByCourse(targetOfferingId);
        List<String> coIds = cos.stream().map(CourseOutcome::getId).collect(Collectors.toList());

        if (!coIds.isEmpty()) {
            coPoMappingRepository.deleteByCourseOutcomeIdIn(coIds);
            coPsoMappingRepository.deleteByCourseOutcomeIdIn(coIds);
            coPoMappingRepository.flush();
            coPsoMappingRepository.flush();
        }

        List<CoPoMapping> savedPo = Collections.emptyList();
        if (dto != null && dto.getPoMappings() != null && !dto.getPoMappings().isEmpty()) {
            Map<String, CoPoMapping> uniquePoMap = new LinkedHashMap<>();
            for (CoPoMapping m : dto.getPoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPoCode();
                if (m.getId() == null || m.getId().isBlank()) {
                    m.setId("copomap-" + UUID.randomUUID().toString().substring(0, 8));
                }
                uniquePoMap.put(key, m);
            }
            savedPo = coPoMappingRepository.saveAll(uniquePoMap.values());
            coPoMappingRepository.flush();
        }

        List<CoPsoMapping> savedPso = Collections.emptyList();
        if (dto != null && dto.getPsoMappings() != null && !dto.getPsoMappings().isEmpty()) {
            Map<String, CoPsoMapping> uniquePsoMap = new LinkedHashMap<>();
            for (CoPsoMapping m : dto.getPsoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPsoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPsoCode();
                if (m.getId() == null || m.getId().isBlank()) {
                    m.setId("copsomap-" + UUID.randomUUID().toString().substring(0, 8));
                }
                uniquePsoMap.put(key, m);
            }
            savedPso = coPsoMappingRepository.saveAll(uniquePsoMap.values());
            coPsoMappingRepository.flush();
        }

        List<ProgrammeOutcome> pos = (batchId != null) ? getPOsByProgramme(batchId) : ((progId != null) ? getPOsByProgramme(progId) : Collections.emptyList());
        List<ProgrammeSpecificOutcome> psos = (batchId != null) ? getPSOsByProgramme(batchId) : ((progId != null) ? getPSOsByProgramme(progId) : Collections.emptyList());

        return CourseMappingMatrixDto.builder()
                .courseId(courseIdOrOfferingId)
                .programmeId(progId)
                .cos(cos)
                .pos(pos)
                .psos(psos)
                .poMappings(savedPo)
                .psoMappings(savedPso)
                .poKeywordsStore(poKwToReturn)
                .psoKeywordsStore(psoKwToReturn)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseOutcome> getOutcomesByOffering(String offeringId) {
        System.out.println("[OutcomeService] getOutcomesByOffering called | offeringId: " + offeringId);
        if (offeringId != null && !offeringId.isBlank()) {
            enforceOfferingScope(offeringId);
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + offeringId));
        return getCOsByCourse(offering.getId());
    }

    @Transactional
    public List<CourseOutcome> saveOutcomesByOffering(String offeringId, List<CourseOutcome> cos) {
        System.out.println("[OutcomeService] saveOutcomesByOffering called | offeringId: " + offeringId);
        if (offeringId != null && !offeringId.isBlank()) {
            enforceOfferingScope(offeringId);
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + offeringId));
        return saveCOs(offering.getId(), cos);
    }

    @Transactional
    public CourseMappingMatrixDto getMappingsByOffering(String offeringId) {
        System.out.println("[OutcomeService] getMappingsByOffering called | offeringId: " + offeringId);
        if (offeringId != null && !offeringId.isBlank()) {
            enforceOfferingScope(offeringId);
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + offeringId));
        return getCourseMappings(offering.getId());
    }

    @Transactional
    public CourseMappingMatrixDto saveMappingsByOffering(String offeringId, CourseMappingMatrixDto dto) {
        System.out.println("[OutcomeService] saveMappingsByOffering called | offeringId: " + offeringId);
        if (offeringId != null && !offeringId.isBlank()) {
            enforceOfferingScope(offeringId);
        }
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + offeringId));
        return saveCourseMappings(offering.getId(), dto);
    }
}
