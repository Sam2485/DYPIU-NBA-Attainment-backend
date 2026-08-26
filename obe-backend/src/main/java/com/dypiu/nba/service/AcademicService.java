package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dypiu.nba.dto.DirectorSetupProgressDto;
import com.dypiu.nba.dto.DirectorSchoolSummaryDto;
import com.dypiu.nba.dto.DepartmentSummaryDto;
import com.dypiu.nba.dto.HodDepartmentSummaryDto;
import com.dypiu.nba.dto.HodSetupProgressDto;
import com.dypiu.nba.dto.ProgrammeCoordinatorSummaryDto;
import com.dypiu.nba.dto.ProgrammeCoordinatorSetupProgressDto;
import com.dypiu.nba.dto.CourseCoordinatorSummaryDto;
import com.dypiu.nba.dto.CourseCoordinatorSetupProgressDto;
import com.dypiu.nba.dto.UserDto;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final StudentRepository studentRepository;
    private final DirectorSetupProgressRepository directorSetupProgressRepository;
    private final HodSetupProgressRepository hodSetupProgressRepository;
    private final ProgrammeCoordinatorSetupProgressRepository pcSetupProgressRepository;
    private final CourseCoordinatorSetupProgressRepository ccSetupProgressRepository;
    private final AttainmentConfigurationRepository configRepository;
    private final BatchLifecycleService batchLifecycleService;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final PoCompetencyRepository poCompetencyRepository;
    private final PsoCompetencyRepository psoCompetencyRepository;
    private final PeoOutcomeRepository peoOutcomeRepository;
    private final UserRepository userRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

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
            if (departmentId != null && scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<Department> hodDepts = departmentRepository.findByHodEmailIgnoreCase(scope.getEmail().trim());
                if (hodDepts != null && !hodDepts.isEmpty()) {
                    boolean match = hodDepts.stream().anyMatch(d -> departmentId.equalsIgnoreCase(d.getId()));
                    if (!match) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned department scope.");
                    }
                    return;
                }
            }
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

        MasterProgramme prog = masterProgrammeRepository.findByIdAndDeletedAtIsNull(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("MasterProgramme not found: " + programmeId));
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
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found: " + batchId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByProgrammeBatchId(batchId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                        ;
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to any MasterCourse Offering in this ProgrammeBatch.");
            }
            return;
        }

        enforceProgrammeScope(batch.getProgrammeId());
    }

    private void enforceCourseScope(String courseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (courseId == null || courseId.isBlank()) return;
        MasterCourse course = masterCourseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("MasterCourse not found: " + courseId));

        if (scope.isFaculty()) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                        ;
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this MasterCourse.");
            }
            return;
        }

        enforceProgrammeScope(course.getProgrammeId());
    }

    private void enforceProgrammeBatchCourseScope(String offeringId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (offeringId == null || offeringId.isBlank()) return;

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("MasterCourse offering not found: " + offeringId));

        if (scope.isFaculty()) {
            boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()))
                    ;
            boolean isFacultyAssigned = isCoordinator || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
            if (!isFacultyAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this MasterCourse Offering.");
            }
            return;
        }

        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
    }

    private void enforceCourseCoordinatorScope(String offeringOrCourseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac() || scope.isDirector() || scope.isHod() || scope.isProgrammeCoordinator()) {
            return;
        }
        if (offeringOrCourseId == null || offeringOrCourseId.isBlank()) return;
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringOrCourseId).orElse(null);
        if (offering == null) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(offeringOrCourseId);
            offering = offerings.stream().findFirst().orElse(null);
        }
        if (offering == null) {
            throw new ResourceNotFoundException("MasterCourse offering not found: " + offeringOrCourseId);
        }
        boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()))
                ;
        if (!isCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned MasterCourse Coordinator can perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public com.dypiu.nba.dto.BatchContextDto getBatchContext(String batchId) {
        System.out.println("[AcademicService] getBatchContext called | batchId: " + batchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found: " + batchId));

        enforceProgrammeScope(batch.getProgrammeId());

        MasterProgramme prog = masterProgrammeRepository.findByIdAndDeletedAtIsNull(batch.getProgrammeId()).orElse(null);
        Department dept = (prog != null && prog.getDepartmentId() != null) ? departmentRepository.findById(prog.getDepartmentId()).orElse(null) : null;
        School school = (dept != null && dept.getSchoolId() != null) ? schoolRepository.findById(dept.getSchoolId()).orElse(null) : null;

        List<Student> students = studentRepository.findByProgrammeBatchId(batchId);
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByProgrammeBatchId(batchId);
        Set<String> uniqueCourseIds = offerings.stream().map(ProgrammeBatchCourse::getCourseId).collect(Collectors.toSet());
        List<String> offeringIds = offerings.stream().map(ProgrammeBatchCourse::getId).collect(Collectors.toList());

        long completedAtrs = offeringIds.isEmpty() ? 0 : courseAtrRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                                                         .filter(a -> a.getStatus() == CourseAtrStatus.VERIFIED)
                                                         .count();

        String progAtrStatus = "DRAFT";
        if (prog != null) {
            Optional<ProgrammeAtr> patr = programmeAtrRepository.findByProgrammeBatchId(batchId);
            if (patr.isPresent() && patr.get().getStatus() != null) {
                progAtrStatus = patr.get().getStatus().name();
            }
        }

        return com.dypiu.nba.dto.BatchContextDto.builder()
                .batch(com.dypiu.nba.dto.BatchContextDto.BatchSummary.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .programmeId(batch.getProgrammeId())
                        .programmeName(batch.getProgrammeName())
                        .status(batch.getStatus())
                        .build())
                .programme(prog != null ? com.dypiu.nba.dto.BatchContextDto.ProgrammeSummary.builder()
                                          .id(prog.getId())
                                          .code(prog.getCode())
                                          .name(prog.getName())
                                          .build() : null)
                .department(dept != null ? com.dypiu.nba.dto.BatchContextDto.DepartmentSummary.builder()
                                           .id(dept.getId())
                                           .name(dept.getName())
                                           .build() : null)
                .school(school != null ? com.dypiu.nba.dto.BatchContextDto.SchoolSummary.builder()
                                         .id(school.getId())
                                         .name(school.getName())
                                         .build() : null)
                .statistics(com.dypiu.nba.dto.BatchContextDto.Statistics.builder()
                        .studentCount(students.size())
                        .courseCount(uniqueCourseIds.size())
                        .courseOfferingCount(offerings.size())
                        .completedCourseAtrCount(completedAtrs)
                        .programmeAtrStatus(progAtrStatus)
                        .build())
                .build();
    }

    private String cleanOverride(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatAssignedFaculty(Object assignedFaculty) {
        if (assignedFaculty == null) return null;
        if (assignedFaculty instanceof java.util.Collection<?> col) {
            if (col.isEmpty()) return null;
            return col.stream().map(Object::toString).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
        }
        String s = assignedFaculty.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public ProgrammeBatchCourse enrichOffering(ProgrammeBatchCourse offering) {
        if (offering == null) return null;

        String codeOverride = cleanOverride(offering.getCourseCodeOverride());
        String nameOverride = cleanOverride(offering.getCourseNameOverride());
        offering.setCourseCodeOverride(codeOverride);
        offering.setCourseNameOverride(nameOverride);

        MasterCourse masterCourse = (offering.getMasterCourseId() != null)
                ? masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null)
                : null;

        if (codeOverride != null) {
            offering.setCourseCode(codeOverride);
        } else if (masterCourse != null) {
            offering.setCourseCode(masterCourse.getCode());
        }

        if (nameOverride != null) {
            offering.setCourseName(nameOverride);
        } else if (masterCourse != null) {
            offering.setCourseName(masterCourse.getName());
        }

        if (offering.getProgrammeBatchId() != null) {
            programmeBatchRepository.findById(offering.getProgrammeBatchId()).ifPresent(batch -> {
                if (offering.getAcademicYear() == null || offering.getAcademicYear().isBlank()) {
                    String ay = (batch.getStartYear() != null && batch.getEndYear() != null)
                            ? (batch.getStartYear() + "-" + (batch.getEndYear() % 100))
                            : null;
                    offering.setAcademicYear(ay);
                }
            });
        }

        if (offering.getCourseCoordinatorId() != null) {
            userRepository.findById(offering.getCourseCoordinatorId()).ifPresent(user -> {
                offering.setCourseCoordinatorName(user.getName());
                offering.setCoordinatorEmail(user.getEmail());
            });
        }

        return offering;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatchCourse> getProgrammeBatchCoursesByBatch(String batchId) {
        System.out.println("[AcademicService] getProgrammeBatchCoursesByProgrammeBatch called | batchId: " + batchId);
        CurrentUserScope scope = getScope();
        List<ProgrammeBatchCourse> offerings;
        if (scope != null && scope.isFaculty()) {
            List<ProgrammeBatchCourse> list = (batchId != null && !batchId.isBlank())
                    ? programmeBatchCourseRepository.findByProgrammeBatchId(batchId)
                    : programmeBatchCourseRepository.findAll();
            offerings = list.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()));
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .collect(Collectors.toList());
        } else if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId);
            offerings = programmeBatchCourseRepository.findByProgrammeBatchId(batchId);
        } else if (scope != null && scope.isProgrammeCoordinator()) {
            List<ProgrammeBatch> batches = getAllBatches();
            Set<String> bIds = batches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            offerings = bIds.isEmpty() ? Collections.emptyList() : programmeBatchCourseRepository.findByProgrammeBatchIdIn(bIds);
        } else if (scope != null && scope.isHod()) {
            List<MasterProgramme> progs = getAllProgrammes();
            List<String> pIds = progs.stream().map(MasterProgramme::getId).toList();
            List<ProgrammeBatch> batches = pIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdIn(pIds);
            Set<String> bIds = batches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            offerings = bIds.isEmpty() ? Collections.emptyList() : programmeBatchCourseRepository.findByProgrammeBatchIdIn(bIds);
        } else if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> dIds = depts.stream().map(Department::getId).toList();
            List<MasterProgramme> progs = dIds.isEmpty() ? Collections.emptyList() : masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(dIds);
            List<String> pIds = progs.stream().map(MasterProgramme::getId).toList();
            List<ProgrammeBatch> batches = pIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdIn(pIds);
            Set<String> bIds = batches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            offerings = bIds.isEmpty() ? Collections.emptyList() : programmeBatchCourseRepository.findByProgrammeBatchIdIn(bIds);
        } else if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            offerings = programmeBatchCourseRepository.findAll();
        } else {
            offerings = (batchId != null && !batchId.isBlank()) ? programmeBatchCourseRepository.findByProgrammeBatchId(batchId) : programmeBatchCourseRepository.findAll();
        }
        offerings.forEach(this::enrichOffering);
        return offerings;
    }

    @Transactional(readOnly = true)
    public ProgrammeBatchCourse getProgrammeBatchCourseById(String offeringId) {
        System.out.println("[AcademicService] getProgrammeBatchCourseById called | offeringId: " + offeringId);
        if (offeringId == null || offeringId.isBlank()) return null;
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        return enrichOffering(offering);
    }

    @Transactional
    public ProgrammeBatchCourse createCourseOffering(com.dypiu.nba.dto.CourseOfferingRequestDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course offering request cannot be null.");
        }
        String rawBatchId = requestDto.getProgrammeBatchId();
        if (rawBatchId == null || rawBatchId.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }
        final String batchId = rawBatchId.trim();

        String rawCourseId = requestDto.getMasterCourseId();
        if (rawCourseId == null || rawCourseId.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "masterCourseId is required.");
        }
        final String masterCourseId = rawCourseId.trim();

        if (requestDto.getSemester() == null || requestDto.getSemester() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "semester is required and must be at least 1.");
        }

        enforceBatchScope(batchId);
        enforceCourseScope(masterCourseId);

        // Verify master course exists without modifying it
        MasterCourse masterCourse = masterCourseRepository.findById(masterCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Master Course not found: " + masterCourseId));

        // Verify programme batch exists
        ProgrammeBatch batch = programmeBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + batchId));

        // Check duplicate offering for same (programmeBatchId, masterCourseId)
        if (programmeBatchCourseRepository.existsByProgrammeBatchIdAndMasterCourseId(batchId, masterCourseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course offering already exists for batch '" + batchId + "' and master course '" + masterCourseId + "'.");
        }

        String codeOverride = cleanOverride(requestDto.getCourseCodeOverride());
        String nameOverride = cleanOverride(requestDto.getCourseNameOverride());

        String assignedFacultyStr = formatAssignedFaculty(requestDto.getAssignedFaculty());

        String coordinatorName = requestDto.getCourseCoordinatorName();
        if (requestDto.getCourseCoordinatorId() != null) {
            User coordUser = userRepository.findById(requestDto.getCourseCoordinatorId()).orElse(null);
            if (coordUser != null) {
                coordinatorName = coordUser.getName();
            }
        }

        ProgrammeBatchCourse offering = ProgrammeBatchCourse.builder()
                .id("offering-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeBatchId(batchId)
                .masterCourseId(masterCourseId)
                .semester(requestDto.getSemester())
                .courseCoordinatorId(requestDto.getCourseCoordinatorId())
                .courseCoordinatorName(coordinatorName)
                .assignedFaculty(assignedFacultyStr)
                .courseCodeOverride(codeOverride)
                .courseNameOverride(nameOverride)
                .status("ACTIVE")
                .build();

        ProgrammeBatchCourse saved = programmeBatchCourseRepository.save(offering);
        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    com.dypiu.nba.audit.AuditAction.CREATE,
                    com.dypiu.nba.audit.ResourceType.PROGRAMME_BATCH_COURSE,
                    saved.getId(),
                    null,
                    "ACTIVE",
                    "Created ProgrammeBatchCourse offering with overrides",
                    java.util.Map.of("masterCourseId", saved.getMasterCourseId(), "programmeBatchId", saved.getProgrammeBatchId())
            );
        }
        return enrichOffering(saved);
    }

    @Transactional
    public ProgrammeBatchCourse updateCourseOffering(String offeringId, com.dypiu.nba.dto.CourseOfferingRequestDto requestDto) {
        if (offeringId == null || offeringId.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offering ID is required.");
        }
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course offering request cannot be null.");
        }
        ProgrammeBatchCourse existing = programmeBatchCourseRepository.findById(offeringId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course offering not found: " + offeringId));

        if (existing.getBatchId() != null) enforceBatchScope(existing.getBatchId());
        if (existing.getCourseId() != null) enforceCourseScope(existing.getCourseId());

        String targetBatchId = existing.getProgrammeBatchId();
        if (requestDto.getProgrammeBatchId() != null && !requestDto.getProgrammeBatchId().trim().isBlank()) {
            targetBatchId = requestDto.getProgrammeBatchId().trim();
            enforceBatchScope(targetBatchId);
            if (!programmeBatchRepository.existsById(targetBatchId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + targetBatchId);
            }
            existing.setProgrammeBatchId(targetBatchId);
        }

        String targetCourseId = existing.getMasterCourseId();
        if (requestDto.getMasterCourseId() != null && !requestDto.getMasterCourseId().trim().isBlank()) {
            targetCourseId = requestDto.getMasterCourseId().trim();
            enforceCourseScope(targetCourseId);
            if (!masterCourseRepository.existsById(targetCourseId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Master Course not found: " + targetCourseId);
            }
            existing.setMasterCourseId(targetCourseId);
        }

        // Check duplicate
        if (programmeBatchCourseRepository.existsByProgrammeBatchIdAndMasterCourseIdAndIdNot(targetBatchId, targetCourseId, existing.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course offering already exists for batch '" + targetBatchId + "' and master course '" + targetCourseId + "'.");
        }

        if (requestDto.getCourseCodeOverride() != null) {
            existing.setCourseCodeOverride(cleanOverride(requestDto.getCourseCodeOverride()));
        }
        if (requestDto.getCourseNameOverride() != null) {
            existing.setCourseNameOverride(cleanOverride(requestDto.getCourseNameOverride()));
        }
        if (requestDto.getSemester() != null) {
            if (requestDto.getSemester() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "semester must be >= 1.");
            }
            existing.setSemester(requestDto.getSemester());
        }
        if (requestDto.getCourseCoordinatorId() != null) {
            existing.setCourseCoordinatorId(requestDto.getCourseCoordinatorId());
            User coordUser = userRepository.findById(requestDto.getCourseCoordinatorId()).orElse(null);
            if (coordUser != null) {
                existing.setCourseCoordinatorName(coordUser.getName());
            }
        }
        if (requestDto.getCourseCoordinatorName() != null && !requestDto.getCourseCoordinatorName().isBlank()) {
            existing.setCourseCoordinatorName(requestDto.getCourseCoordinatorName().trim());
        }
        if (requestDto.getAssignedFaculty() != null) {
            existing.setAssignedFaculty(formatAssignedFaculty(requestDto.getAssignedFaculty()));
        }

        ProgrammeBatchCourse saved = programmeBatchCourseRepository.save(existing);
        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    com.dypiu.nba.audit.AuditAction.UPDATE,
                    com.dypiu.nba.audit.ResourceType.PROGRAMME_BATCH_COURSE,
                    saved.getId(),
                    null,
                    saved.getStatus(),
                    "Updated ProgrammeBatchCourse offering",
                    java.util.Map.of("masterCourseId", saved.getMasterCourseId(), "programmeBatchId", saved.getProgrammeBatchId())
            );
        }
        return enrichOffering(saved);
    }

    @Transactional
    public ProgrammeBatchCourse saveProgrammeBatchCourse(ProgrammeBatchCourse offering) {
        System.out.println("[AcademicService] saveProgrammeBatchCourse called | courseId: " + (offering != null ? offering.getCourseId() : "null"));
        if (offering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterCourse offering details cannot be null.");
        }
        if (offering.getBatchId() != null) {
            enforceBatchScope(offering.getBatchId());
            batchLifecycleService.enforceBatchEditability(offering.getBatchId());
        }
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        if (offering.getId() != null) {
            ProgrammeBatchCourse existing = programmeBatchCourseRepository.findById(offering.getId()).orElse(null);
            if (existing != null) {
                if (existing.getBatchId() != null) enforceBatchScope(existing.getBatchId());
                if (existing.getCourseId() != null) enforceCourseScope(existing.getCourseId());
            }
        }
        if (offering.getId() == null || offering.getId().isBlank()) {
            offering.setId("offering-" + UUID.randomUUID().toString().substring(0, 8));
        }
        boolean isNew = !programmeBatchCourseRepository.existsById(offering.getId());
        if (isNew && (offering.getStatus() == null || offering.getStatus().isBlank())) {
            offering.setStatus("ACTIVE");
        }
        offering.setCourseCodeOverride(cleanOverride(offering.getCourseCodeOverride()));
        offering.setCourseNameOverride(cleanOverride(offering.getCourseNameOverride()));
        ProgrammeBatchCourse saved = programmeBatchCourseRepository.save(offering);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNew ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.PROGRAMME_BATCH_COURSE, saved.getId(), null, saved.getStatus(), isNew ? "Created ProgrammeBatchCourse" : "Updated ProgrammeBatchCourse", java.util.Map.of("courseId", saved.getCourseId() != null ? saved.getCourseId() : ""));
        }
        return enrichOffering(saved);
    }

    @Transactional
    public void deleteProgrammeBatchCourse(String id) {
        System.out.println("[AcademicService] deleteProgrammeBatchCourse called | id: " + id);
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MasterCourse offering not found: " + id));
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        programmeBatchCourseRepository.deleteById(id);
    }

    // --- Director School Summary ---
    @Transactional(readOnly = true)
    public DirectorSchoolSummaryDto getDirectorSchoolSummary(String directorEmail) {
        System.out.println("[AcademicService] Starting school summary fetch for directorEmail: " + directorEmail);
        CurrentUserScope scope = getScope();
        Optional<School> schoolOpt = Optional.empty();

        if (scope != null && scope.isDirector()) {
            schoolOpt = schoolRepository.findById(scope.getRequiredSchoolId());
        } else {
            if (directorEmail != null && !directorEmail.isBlank()) {
                String cleanEmail = directorEmail.trim();
                schoolOpt = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
                if (schoolOpt.isEmpty()) {
                    Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (user.getSchoolId() != null && !user.getSchoolId().isBlank()) {
                            schoolOpt = schoolRepository.findById(user.getSchoolId());
                        }
                        if (schoolOpt.isEmpty() && user.getId() != null) {
                            schoolOpt = schoolRepository.findByDirectorId(user.getId());
                        }
                    }
                }
            }
            if (schoolOpt.isEmpty() && scope != null && scope.getSchoolId() != null) {
                schoolOpt = schoolRepository.findById(scope.getSchoolId());
            }
        }

        if (schoolOpt.isEmpty()) {
            System.out.println("[AcademicService] No school found in database.");
            return DirectorSchoolSummaryDto.builder()
                    .schoolId(null)
                    .schoolName(null)
                    .schoolCode(null)
                    .directorName(null)
                    .directorEmail(directorEmail)
                    .estYear(null)
                    .totalDepartments(0)
                    .assignedHODsCount(0)
                    .unassignedHODsCount(0)
                    .totalProgrammes(0)
                    .build();
        }

        School school = schoolOpt.get();
        List<Department> departments = departmentRepository.findBySchoolId(school.getId());
        List<String> deptIds = departments.stream().map(Department::getId).toList();
        List<MasterProgramme> schoolProgrammes = deptIds.isEmpty() ? Collections.emptyList() : masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds);

        int assignedHodCount = 0;
        int unassignedHodCount = 0;

        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            if (isHodAssigned) {
                assignedHodCount++;
            } else {
                unassignedHodCount++;
            }
        }

        String dName = school.getDirectorName() != null && !school.getDirectorName().isBlank()
                ? school.getDirectorName()
                : school.getDirector();

        DirectorSchoolSummaryDto summary = DirectorSchoolSummaryDto.builder()
                .schoolId(school.getId())
                .schoolName(school.getName())
                .schoolCode(school.getCode())
                .directorName(dName)
                .directorEmail(school.getDirectorEmail())
                .estYear(school.getEstYear())
                .totalDepartments(departments.size())
                .assignedHODsCount(assignedHodCount)
                .unassignedHODsCount(unassignedHodCount)
                .totalProgrammes(schoolProgrammes.size())
                .build();

        System.out.println("[AcademicService] Fetched director school summary for school: " + school.getName() + " (ID: " + school.getId() + ")");
        return summary;
    }

    // --- Director Department Summary ---
    @Transactional(readOnly = true)
    public List<DepartmentSummaryDto> getDepartmentSummary(String schoolId, String directorEmail) {
        System.out.println("[AcademicService] getDepartmentSummary called | schoolId: " + schoolId + " | directorEmail: " + directorEmail);
        CurrentUserScope scope = getScope();
        String targetSchoolId = null;

        if (scope != null && scope.isDirector()) {
            targetSchoolId = scope.getRequiredSchoolId();
        } else if (schoolId != null && !schoolId.isBlank() && !schoolId.equals("sch-1")) {
            targetSchoolId = schoolId;
        } else if (directorEmail != null && !directorEmail.isBlank()) {
            Optional<School> schOpt = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail.trim());
            if (schOpt.isPresent()) {
                targetSchoolId = schOpt.get().getId();
            }
        } else if (scope != null && scope.getSchoolId() != null) {
            targetSchoolId = scope.getSchoolId();
        }

        if (targetSchoolId == null) {
            return Collections.emptyList();
        }

        enforceSchoolScope(targetSchoolId);

        List<Department> departments = departmentRepository.findBySchoolId(targetSchoolId);

        List<DepartmentSummaryDto> list = new ArrayList<>();
        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            int progsCount = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(dept.getId()).size();
            list.add(DepartmentSummaryDto.builder()
                    .deptId(dept.getId())
                    .deptCode(dept.getCode())
                    .deptName(dept.getName())
                    .deptHodName(dept.getHod())
                    .deptHodEmail(dept.getHodEmail())
                    .hodAssignedStatus(isHodAssigned)
                    .programmesCount(progsCount)
                    .build());
        }

        System.out.println("[AcademicService] Fetched department summary list (" + list.size() + " items) for schoolId: " + targetSchoolId);
        return list;
    }

    // --- Director Setup Progress ---
    @Transactional(readOnly = true)
    public DirectorSetupProgressDto getDirectorSetupProgress(String schoolId, String directorEmail) {
        System.out.println("[AcademicService] getDirectorSetupProgress called | schoolId: " + schoolId + " | directorEmail: " + directorEmail);
        CurrentUserScope scope = getScope();
        if (schoolId != null && !schoolId.isBlank()) {
            enforceSchoolScope(schoolId.trim());
        }

        String targetSchoolId = null;

        if (scope != null && scope.isDirector()) {
            targetSchoolId = scope.getRequiredSchoolId();
        } else if (schoolId != null && !schoolId.isBlank() && !schoolId.equals("sch-1")) {
            targetSchoolId = schoolId;
        } else if (directorEmail != null && !directorEmail.isBlank()) {
            Optional<School> schOpt = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail.trim());
            if (schOpt.isPresent()) {
                targetSchoolId = schOpt.get().getId();
            }
        } else if (scope != null && scope.getSchoolId() != null) {
            targetSchoolId = scope.getSchoolId();
        }

        if (targetSchoolId == null) {
            return null;
        }

        enforceSchoolScope(targetSchoolId);

        final String finalSchoolId = targetSchoolId;
        DirectorSetupProgress progress = directorSetupProgressRepository.findBySchoolId(finalSchoolId)
                .orElseGet(() -> DirectorSetupProgress.builder()
                        .id("progress-" + finalSchoolId)
                        .schoolId(finalSchoolId)
                        .currentStep(1)
                        .currentStepEnum(DirectorSetupStep.SCHOOL)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("school,department,programme,review")
                        .updatedAt(ZonedDateTime.now())
                        .build());

        DirectorSetupProgressDto dto = buildSetupProgressDto(progress);
        System.out.println("[AcademicService] Fetched director setup progress for schoolId: " + finalSchoolId + " at step: " + progress.getCurrentStep() + ", completedSteps: " + dto.getCompletedSteps());
        return dto;
    }

    private String normalizeDirectorStepName(Object obj) {
        if (obj == null) return null;
        String s = String.valueOf(obj).trim().toLowerCase();
        if (s.equals("1") || s.equals("school") || s.equals("school_structure")) return "school";
        if (s.equals("2") || s.equals("department") || s.equals("department_management")) return "department";
        if (s.equals("3") || s.equals("programme") || s.equals("programme_overview")) return "programme";
        if (s.equals("4") || s.equals("review") || s.equals("governance")) return "review";
        return s;
    }

    private DirectorSetupStep toDirectorSetupStep(int stepNumber) {
        switch (stepNumber) {
            case 2: return DirectorSetupStep.DEPARTMENT;
            case 3: return DirectorSetupStep.PROGRAMME;
            case 4: return DirectorSetupStep.REVIEW;
            default: return DirectorSetupStep.SCHOOL;
        }
    }

    @Transactional
    public DirectorSetupProgressDto updateDirectorSetupProgress(
            String schoolId,
            Integer stepNumber) {
        return updateDirectorSetupProgress(schoolId, stepNumber, null, null);
    }

    @Transactional
    public DirectorSetupProgressDto updateDirectorSetupProgress(
            String schoolId,
            Integer targetStep,
            String completedStep,
            List<String> completedStepsList) {
        System.out.println("[AcademicService] updateDirectorSetupProgress called | schoolId: " + schoolId + " | targetStep: " + targetStep + " | completedStep: " + completedStep + " | completedStepsList: " + completedStepsList);

        CurrentUserScope scope = getScope();
        if (schoolId != null && !schoolId.isBlank()) {
            enforceSchoolScope(schoolId.trim());
        }

        String targetSchoolId = null;
        if (scope != null && scope.isDirector()) {
            targetSchoolId = scope.getRequiredSchoolId();
        } else if (schoolId != null && !schoolId.isBlank() && !schoolId.equals("sch-1")) {
            targetSchoolId = schoolId;
        } else if (scope != null && scope.getSchoolId() != null) {
            targetSchoolId = scope.getSchoolId();
        }

        if (targetSchoolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School scope cannot be determined.");
        }

        enforceSchoolScope(targetSchoolId);

        final String finalSchoolId = targetSchoolId;
        DirectorSetupProgress progress =
                directorSetupProgressRepository
                        .findBySchoolId(finalSchoolId)
                        .orElseGet(() -> DirectorSetupProgress.builder()
                                .id("progress-" + finalSchoolId)
                                .schoolId(finalSchoolId)
                                .build());

        Set<String> existingCompleted = new LinkedHashSet<>();
        if (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()) {
            for (String s : progress.getCompletedSteps().split(",")) {
                String norm = normalizeDirectorStepName(s);
                if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
            }
        }

        // Add newly completed step(s)
        if (completedStepsList != null && !completedStepsList.isEmpty()) {
            for (Object item : completedStepsList) {
                String norm = normalizeDirectorStepName(item);
                if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
            }
        } else if (completedStep != null && !completedStep.isBlank()) {
            String norm = normalizeDirectorStepName(completedStep);
            if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
        } else if (targetStep != null) {
            String norm = normalizeDirectorStepName(targetStep);
            if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
        }

        List<String> ALL_STEPS = List.of("school", "department", "programme", "review");
        List<String> completed = ALL_STEPS.stream().filter(existingCompleted::contains).toList();
        List<String> pending = ALL_STEPS.stream().filter(s -> !existingCompleted.contains(s)).toList();

        int currentStep = (targetStep != null && targetStep >= 1 && targetStep <= 4)
                ? targetStep
                : (progress.getCurrentStep() != null ? progress.getCurrentStep() : 1);

        DirectorSetupStep stepEnum = toDirectorSetupStep(currentStep);

        SetupStepStatus overallStatus;
        if (completed.size() == ALL_STEPS.size()) {
            overallStatus = SetupStepStatus.COMPLETED;
        } else if (completed.isEmpty()) {
            overallStatus = SetupStepStatus.NOT_STARTED;
        } else {
            overallStatus = SetupStepStatus.IN_PROGRESS;
        }

        progress.setCurrentStep(currentStep);
        progress.setCurrentStepEnum(stepEnum);
        progress.setOverallStatus(overallStatus);
        progress.setCompletedSteps(String.join(",", completed));
        progress.setPendingSteps(String.join(",", pending));
        progress.setUpdatedAt(ZonedDateTime.now());

        directorSetupProgressRepository.save(progress);

        DirectorSetupProgressDto dto = buildSetupProgressDto(progress);
        System.out.println(
                "[AcademicService] Director setup progress updated | " +
                        "schoolId=" + targetSchoolId +
                        " | currentStep=" + currentStep +
                        " | completed=" + completed +
                        " | pending=" + pending +
                        " | overallStatus=" + overallStatus
        );

        return dto;
    }

    private DirectorSetupProgressDto buildSetupProgressDto(DirectorSetupProgress progress) {
        List<String> completedList = (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank())
                ? Arrays.asList(progress.getCompletedSteps().split(","))
                : Collections.emptyList();

        List<String> pendingList = (progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank())
                ? Arrays.asList(progress.getPendingSteps().split(","))
                : Collections.emptyList();

        Map<DirectorSetupStep, SetupStepStatus> stepStatuses = new EnumMap<>(DirectorSetupStep.class);
        int currentStep = progress.getCurrentStep();

        boolean isSchoolDone = completedList.contains("school");
        boolean isDeptDone = completedList.contains("department");
        boolean isProgDone = completedList.contains("programme");
        boolean isReviewDone = completedList.contains("review");

        stepStatuses.put(DirectorSetupStep.SCHOOL, isSchoolDone ? SetupStepStatus.COMPLETED : (currentStep == 1 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.DEPARTMENT, isDeptDone ? SetupStepStatus.COMPLETED : (currentStep == 2 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.PROGRAMME, isProgDone ? SetupStepStatus.COMPLETED : (currentStep == 3 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.REVIEW, isReviewDone ? SetupStepStatus.COMPLETED : SetupStepStatus.NOT_STARTED);

        return DirectorSetupProgressDto.builder()
                .currentStep(progress.getCurrentStep())
                .currentStepEnum(progress.getCurrentStepEnum())
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completedList)
                .pendingSteps(pendingList)
                .stepStatuses(stepStatuses)
                .schoolId(progress.getSchoolId())
                .build();
    }

    // --- Schools ---
    @Transactional(readOnly = true)
    public List<School> getAllSchools() {
        System.out.println("[AcademicService] getAllSchools called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            return schoolRepository.findById(scope.getRequiredSchoolId())
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        if (scope != null && scope.isHod()) {
            return schoolRepository.findById(scope.getRequiredSchoolId())
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        List<School> schools = schoolRepository.findAll();
        System.out.println("[AcademicService] Fetched all schools list (" + schools.size() + " items)");
        return schools;
    }

    @Transactional(readOnly = true)
    public School getSchoolById(String id) {
        System.out.println("[AcademicService] getSchoolById called | id: " + id);
        enforceSchoolScope(id);
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        System.out.println("[AcademicService] Fetched school by id: " + id);
        return school;
    }

    @Transactional
    public School saveSchool(School school) {
        System.out.println("[AcademicService] saveSchool called | school: " + (school != null ? school.getName() : "null"));
        if (school == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School details cannot be null.");
        }
        if (school.getId() != null) {
            enforceSchoolScope(school.getId());
        }

        // 1. Check if directorId is already mapped to another school
        if (school.getDirectorId() != null) {
            Optional<School> existingByDirectorId = schoolRepository.findByDirectorId(school.getDirectorId());
            if (existingByDirectorId.isPresent()) {
                School existing = existingByDirectorId.get();
                if (school.getId() == null || !existing.getId().equalsIgnoreCase(school.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Director is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
                }
            }
        }

        // 2. Check if directorEmail is already mapped to another school
        if (school.getDirectorEmail() != null && !school.getDirectorEmail().isBlank()) {
            String cleanEmail = school.getDirectorEmail().trim();
            Optional<School> existingByEmail = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
            if (existingByEmail.isPresent()) {
                School existing = existingByEmail.get();
                if (school.getId() == null || !existing.getId().equalsIgnoreCase(school.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Director with email '" + cleanEmail + "' is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
                }
            }

            // Sync directorId and directorName from User entity if available
            userRepository.findByEmail(cleanEmail).ifPresent(u -> {
                if (school.getDirectorId() == null) {
                    school.setDirectorId(u.getId());
                }
                if (school.getDirectorName() == null || school.getDirectorName().isBlank()) {
                    school.setDirectorName(u.getName());
                }
            });
        }

        if (school.getId() == null || school.getId().isBlank()) {
            school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        }

        boolean isNewSchool = (school.getId() == null || !schoolRepository.existsById(school.getId()));
        School saved = schoolRepository.save(school);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNewSchool ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.SCHOOL, saved.getId(), null, "ACTIVE", isNewSchool ? "Created School" : "Updated School", java.util.Map.of("code", saved.getCode() != null ? saved.getCode() : "", "name", saved.getName() != null ? saved.getName() : ""));
        }

        // Sync schoolId to the Director user in userRepository
        if (saved.getDirectorEmail() != null && !saved.getDirectorEmail().isBlank()) {
            userRepository.findByEmail(saved.getDirectorEmail().trim()).ifPresent(u -> {
                u.setSchoolId(saved.getId());
                userRepository.save(u);
                System.out.println("[AcademicService] Associated director user (" + u.getEmail() + ") with school: " + saved.getId());
            });
        }

        System.out.println("[AcademicService] Saved school with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public School updateSchool(String id, School schoolDetails) {
        System.out.println("[AcademicService] updateSchool called | id: " + id + " | name: " + (schoolDetails != null ? schoolDetails.getName() : "null"));
        enforceSchoolScope(id);
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));

        // 1. Validate Director ID Uniqueness
        if (schoolDetails.getDirectorId() != null) {
            Optional<School> existingByDirectorId = schoolRepository.findByDirectorId(schoolDetails.getDirectorId());
            if (existingByDirectorId.isPresent() && !existingByDirectorId.get().getId().equalsIgnoreCase(id)) {
                School existing = existingByDirectorId.get();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Director is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
            }
            school.setDirectorId(schoolDetails.getDirectorId());
        }

        // 2. Validate Director Email Uniqueness
        if (schoolDetails.getDirectorEmail() != null && !schoolDetails.getDirectorEmail().isBlank()) {
            String cleanEmail = schoolDetails.getDirectorEmail().trim();
            Optional<School> existingByEmail = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
            if (existingByEmail.isPresent() && !existingByEmail.get().getId().equalsIgnoreCase(id)) {
                School existing = existingByEmail.get();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Director with email '" + cleanEmail + "' is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
            }
            school.setDirectorEmail(cleanEmail);

            // Sync directorId and directorName from User entity if available
            userRepository.findByEmail(cleanEmail).ifPresent(u -> {
                if (school.getDirectorId() == null) {
                    school.setDirectorId(u.getId());
                }
                if (schoolDetails.getDirectorName() == null || schoolDetails.getDirectorName().isBlank()) {
                    school.setDirectorName(u.getName());
                }
            });
        }

        if (schoolDetails.getName() != null && !schoolDetails.getName().isBlank()) {
            school.setName(schoolDetails.getName());
        }
        if (schoolDetails.getCode() != null && !schoolDetails.getCode().isBlank()) {
            school.setCode(schoolDetails.getCode());
        }
        if (schoolDetails.getDirectorName() != null) {
            school.setDirectorName(schoolDetails.getDirectorName());
        } else if (schoolDetails.getDirector() != null) {
            school.setDirector(schoolDetails.getDirector());
        }
        if (schoolDetails.getEstYear() != null) {
            school.setEstYear(schoolDetails.getEstYear());
        }

        School updated = schoolRepository.save(school);
        if (auditLogService != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.SCHOOL, updated.getId(), null, "ACTIVE", "Updated School Details", java.util.Map.of("code", updated.getCode() != null ? updated.getCode() : "", "name", updated.getName() != null ? updated.getName() : ""));
        }

        // Sync schoolId to the Director user in userRepository
        if (updated.getDirectorEmail() != null && !updated.getDirectorEmail().isBlank()) {
            userRepository.findByEmail(updated.getDirectorEmail().trim()).ifPresent(u -> {
                u.setSchoolId(updated.getId());
                userRepository.save(u);
            });
        }

        System.out.println("[AcademicService] Updated school info for id: " + updated.getId());
        return updated;
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        System.out.println("[AcademicService] getAllDepartments called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            return departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
        }
        if (scope != null && scope.isHod()) {
            List<Department> byEmail = (scope.getEmail() != null && !scope.getEmail().isBlank())
                    ? departmentRepository.findByHodEmailIgnoreCase(scope.getEmail().trim())
                    : Collections.emptyList();
            if (byEmail != null && !byEmail.isEmpty()) {
                return byEmail;
            }
            return departmentRepository.findById(scope.getRequiredDepartmentId())
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        List<Department> list = departmentRepository.findAll();
        System.out.println("[AcademicService] Fetched all departments (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Department> getDepartmentsBySchool(String schoolId) {
        System.out.println("[AcademicService] getDepartmentsBySchool called | schoolId: " + schoolId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            String dirSchoolId = scope.getRequiredSchoolId();
            if (schoolId != null && !schoolId.isBlank() && !schoolId.equals(dirSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot view departments of a different school.");
            }
            return departmentRepository.findBySchoolId(dirSchoolId);
        }
        if (scope != null && scope.isHod()) {
            List<Department> byEmail = (scope.getEmail() != null && !scope.getEmail().isBlank())
                    ? departmentRepository.findByHodEmailIgnoreCase(scope.getEmail().trim())
                    : Collections.emptyList();
            if (byEmail != null && !byEmail.isEmpty()) {
                if (schoolId != null && !schoolId.isBlank()) {
                    return byEmail.stream().filter(d -> schoolId.equals(d.getSchoolId())).toList();
                }
                return byEmail;
            }
            return departmentRepository.findById(scope.getRequiredDepartmentId())
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        List<Department> list = departmentRepository.findBySchoolId(schoolId);
        System.out.println("[AcademicService] Fetched departments (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(String id) {
        System.out.println("[AcademicService] getDepartmentById called | id: " + id);
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        enforceSchoolScope(dept.getSchoolId());
        enforceDepartmentScope(dept.getId());
        return dept;
    }

    @Transactional
    public Department saveDepartment(Department department) {
        System.out.println("[AcademicService] saveDepartment called | department: " + (department != null ? department.getName() : "null"));
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            department.setSchoolId(scope.getRequiredSchoolId());
        }
        if (department.getId() != null) {
            Department existing = departmentRepository.findById(department.getId()).orElse(null);
            if (existing != null) {
                enforceSchoolScope(existing.getSchoolId());
                enforceDepartmentScope(existing.getId());
            }
        }
        if (department.getId() == null) department.setId("dept-" + UUID.randomUUID().toString().substring(0, 8));
        boolean isNewDept = (department.getId() == null || !departmentRepository.existsById(department.getId()));
        Department saved = departmentRepository.save(department);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNewDept ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.DEPARTMENT, saved.getId(), null, "ACTIVE", isNewDept ? "Created Department" : "Updated Department", java.util.Map.of("code", saved.getCode() != null ? saved.getCode() : "", "name", saved.getName() != null ? saved.getName() : ""));
        }
        System.out.println("[AcademicService] Saved department with id: " + saved.getId());

        // Sync department info to HOD user if hodEmail or hod name matches
        if (saved.getHodEmail() != null && !saved.getHodEmail().isBlank()) {
            userRepository.findByEmail(saved.getHodEmail()).ifPresent(user -> {
                user.setDepartment(saved.getName());
                user.setDepartmentId(saved.getId());
                if (saved.getSchoolId() != null) {
                    user.setSchoolId(saved.getSchoolId());
                }
                userRepository.save(user);
                System.out.println("[AcademicService] Updated HOD user (" + user.getEmail() + ") department to: " + saved.getName());
            });
        }
        return saved;
    }

    @Transactional
    public void deleteDepartment(String id) {
        System.out.println("[AcademicService] deleteDepartment called | id: " + id);
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        enforceSchoolScope(dept.getSchoolId());
        enforceDepartmentScope(dept.getId());
        departmentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted department with id: " + id);
    }

    // --- Users by Role ---
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(String role) {
        System.out.println("[AcademicService] getUsersByRole called | role: " + role);
        CurrentUserScope scope = getScope();
        List<User> users;

        if (role == null || role.isBlank() || role.equalsIgnoreCase("ALL")) {
            users = userRepository.findAll();
        } else {
            String searchRole = role.trim().toUpperCase().replace("-", "_");
            if (searchRole.equals("PROGRAMME_COORDINATOR")
                    || searchRole.equals("COORDINATOR")
                    || searchRole.equals("PC")
                    || searchRole.equals("PROGRAMME_COORD")) {
                users = userRepository.findByRole(UserRole.PROGRAMME_COORDINATOR);
            } else if (searchRole.equals("COURSE_COORDINATOR")
                    || searchRole.equals("CC")
                    || searchRole.equals("FACULTY")) {
                users = userRepository.findByRole(UserRole.FACULTY);
            } else if (searchRole.equals("HOD")) {
                users = userRepository.findByRole(UserRole.HOD);
            } else if (searchRole.equals("DIRECTOR")) {
                users = userRepository.findByRole(UserRole.DIRECTOR);
            } else if (searchRole.equals("IQAC")) {
                users = userRepository.findByRole(UserRole.IQAC);
            } else {
                try {
                    UserRole userRole = UserRole.valueOf(searchRole);
                    users = userRepository.findByRole(userRole);
                } catch (IllegalArgumentException e) {
                    users = userRepository.findAll();
                }
            }
        }

        // Apply organizational scope isolation for Director, HOD, and MasterProgramme Coordinator
        if (scope != null && scope.isDirector()) {
            String schoolId = scope.getRequiredSchoolId();
            users = users.stream()
                    .filter(u -> u.getSchoolId() != null && u.getSchoolId().equals(schoolId))
                    .collect(Collectors.toList());
        } else if (scope != null && scope.isHod()) {
            String schoolId = scope.getRequiredSchoolId();
            String deptId = scope.getRequiredDepartmentId();
            users = users.stream()
                    .filter(u -> (u.getSchoolId() == null || u.getSchoolId().equals(schoolId))
                            && (u.getDepartmentId() == null || u.getDepartmentId().equals(deptId)))
                    .collect(Collectors.toList());
        } else if (scope != null && scope.isProgrammeCoordinator()) {
            if (scope.hasSchoolScope()) {
                String schoolId = scope.getSchoolId();
                users = users.stream().filter(u -> u.getSchoolId() == null || u.getSchoolId().equals(schoolId)).collect(Collectors.toList());
            }
            if (scope.hasDepartmentScope()) {
                String deptId = scope.getDepartmentId();
                users = users.stream().filter(u -> u.getDepartmentId() == null || u.getDepartmentId().equals(deptId)).collect(Collectors.toList());
            }
        }

        List<UserDto> dtos = users.stream()
                .map(u -> {
                    String resolvedEmail = u.getEmail();
                    if (resolvedEmail == null || resolvedEmail.isBlank()) {
                        if (u.getUsername() != null && u.getUsername().contains("@")) {
                            resolvedEmail = u.getUsername();
                        } else if (u.getUsername() != null && !u.getUsername().isBlank()) {
                            resolvedEmail = u.getUsername() + "@dypiu.ac.in";
                        } else {
                            resolvedEmail = "user" + u.getId() + "@dypiu.ac.in";
                        }
                    }
                    return UserDto.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .name(u.getName() != null && !u.getName().isBlank() ? u.getName() : (u.getUsername() != null ? u.getUsername() : "User " + u.getId()))
                            .email(resolvedEmail)
                            .role(u.getRole() != null
                                    ? u.getRole().name()
                                    : UserRole.FACULTY.name())
                            .schoolId(u.getSchoolId())
                            .departmentId(u.getDepartmentId())
                            .masterProgrammeId(u.getProgrammeId())
                            .department(u.getDepartment())
                            .programme(u.getProgramme())
                            .build();
                })
                .toList();

        System.out.println("[AcademicService] Fetched users by role (" + role + "): count=" + dtos.size());
        return dtos;
    }

    // --- Programmes ---
        @Transactional(readOnly = true)
    private void enrichProgrammeCoordinator(MasterProgramme programme) {
        if (programme == null) return;
        String coordEmail = programme.getCoordinatorEmail();
        String coord = programme.getCoordinator();

        // 1. If coordinatorEmail is valid, lookup user by email
        if (coordEmail != null && !coordEmail.isBlank() && coordEmail.contains("@")) {
            Optional<User> uOpt = userRepository.findByEmail(coordEmail.trim());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                programme.setCoordinator(u.getName());
                programme.setCoordinatorEmail(u.getEmail());
                return;
            }
        }

        // 1. If coordinatorEmail is valid, lookup user by email
        if (coordEmail != null && !coordEmail.isBlank() && coordEmail.contains("@")) {
            Optional<User> uOpt = userRepository.findByEmail(coordEmail.trim());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                programme.setCoordinator(u.getName());
                programme.setCoordinatorEmail(u.getEmail());
                return;
            }
        }

        // 2. Lookup by coordinator string (ID, email, username, name)
        // 3. Fallback: check if a user with PROGRAMME_COORDINATOR role is assigned to this programme
        if (programme.getCoordinator() == null || programme.getCoordinator().isBlank() || "Not Assigned".equalsIgnoreCase(programme.getCoordinator())) {
            List<User> pcs = userRepository.findByRole(UserRole.PROGRAMME_COORDINATOR).stream().filter(u -> programme.getId().equalsIgnoreCase(u.getProgrammeId())).toList();
            if (pcs != null && !pcs.isEmpty()) {
                programme.setCoordinator(pcs.get(0).getName());
                programme.setCoordinatorEmail(pcs.get(0).getEmail());
                return;
            }
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(programme.getId());
            for (ProgrammeBatch b : batches) {
                if (b.getCoordinatorName() != null && !b.getCoordinatorName().isBlank()) {
                    programme.setCoordinator(b.getCoordinatorName());
                    programme.setCoordinatorEmail(b.getCoordinatorEmail());
                    return;
                }
            }
        }

        if (coord != null && !coord.isBlank()) {
            if (coord.matches("\\d+")) {
                try {
                    Long userId = Long.parseLong(coord);
                    userRepository.findById(userId).ifPresent(u -> {
                        programme.setCoordinator(u.getName());
                        if (programme.getCoordinatorEmail() == null || programme.getCoordinatorEmail().isBlank()) {
                            programme.setCoordinatorEmail(u.getEmail());
                        }
                    });
                } catch (NumberFormatException ignored) {}
            } else if (coord.contains("@")) {
                userRepository.findByEmail(coord.trim()).ifPresent(u -> {
                    programme.setCoordinator(u.getName());
                    programme.setCoordinatorEmail(u.getEmail());
                });
            } else {
                Optional<User> uOpt = userRepository.findByUsername(coord.trim());
                if (uOpt.isEmpty()) {
                    uOpt = userRepository.findAll().stream()
                            .filter(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(coord.trim()))
                            .findFirst();
                }
                if (uOpt.isPresent()) {
                    User u = uOpt.get();
                    programme.setCoordinator(u.getName());
                    if (programme.getCoordinatorEmail() == null || programme.getCoordinatorEmail().isBlank()) {
                        programme.setCoordinatorEmail(u.getEmail());
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSummaryDto getCourseCoordinatorSummary(String coordinatorEmail) {
        return getCourseCoordinatorSummary(coordinatorEmail, null);
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSummaryDto getCourseCoordinatorSummary(String coordinatorEmail, String selectedCourseOrOfferingId) {
        System.out.println("[AcademicService] getCourseCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail + " | selectedCourseOrOfferingId: " + selectedCourseOrOfferingId);
        CurrentUserScope scope = getScope();
        String name = scope != null && scope.getName() != null ? scope.getName() : "Course Coordinator";
        String email = scope != null && scope.getEmail() != null ? scope.getEmail() : (coordinatorEmail != null ? coordinatorEmail.trim() : "");
        Long userId = scope != null ? scope.getUserId() : null;

        if (scope == null || !scope.isFaculty()) {
            if (!email.isBlank()) {
                Optional<User> uOpt = userRepository.findByEmail(email);
                if (uOpt.isPresent()) {
                    name = uOpt.get().getName();
                    userId = uOpt.get().getId();
                }
            }
        }

        List<ProgrammeBatchCourse> allOfferings = programmeBatchCourseRepository.findAll();
        final String searchEmail = email.toLowerCase();
        final String searchName = name.toLowerCase();

        List<ProgrammeBatchCourse> assignedOfferings = allOfferings.stream()
                .filter(o -> {
                    boolean matches = false;
                    if (o.getCourseCoordinatorId() != null) {
                        User u = userRepository.findById(o.getCourseCoordinatorId()).orElse(null);
                        if (u != null && (u.getName().equalsIgnoreCase(searchName) || u.getEmail().equalsIgnoreCase(searchName))) {
                            matches = true;
                        }
                    }
                    boolean matchFaculty = (o.getAssignedFaculty() != null && (o.getAssignedFaculty().toLowerCase().contains(searchEmail) || (!searchName.isBlank() && o.getAssignedFaculty().toLowerCase().contains(searchName))));
                    return matches || matchFaculty;
                })
                .toList();

        Set<String> assignedCourseIds = assignedOfferings.stream().map(ProgrammeBatchCourse::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<MasterCourse> finalCourses = assignedCourseIds.isEmpty() ? Collections.emptyList() : masterCourseRepository.findAllById(assignedCourseIds);

        for (MasterCourse course : finalCourses) {
            ProgrammeBatchCourse offering = assignedOfferings.stream()
                    .filter(o -> course.getId().equals(o.getCourseId()))
                    .findFirst()
                    .orElse(null);
            if (offering != null) {
                course.setProgrammeBatchCourseId(offering.getId());
                course.setSemester(offering.getSemester() != null ? String.valueOf(offering.getSemester()) : null);
                course.setCoordinator(offering.getCourseCoordinatorName());
                course.setFaculty(offering.getCourseCoordinatorName());
                course.setAssignedFaculty(offering.getAssignedFaculty());
                if (offering.getAssignedFaculty() != null && offering.getAssignedFaculty().contains("(")) {
                    String emailPart = offering.getAssignedFaculty().substring(offering.getAssignedFaculty().indexOf("(") + 1, offering.getAssignedFaculty().indexOf(")"));
                    course.setCoordinatorEmail(emailPart);
                }
            }
        }

        // Determine target offering for detailed metrics
        ProgrammeBatchCourse targetOffering = null;
        if (selectedCourseOrOfferingId != null && !selectedCourseOrOfferingId.isBlank()) {
            if (programmeBatchCourseRepository.existsById(selectedCourseOrOfferingId)) {
                targetOffering = programmeBatchCourseRepository.findById(selectedCourseOrOfferingId).orElse(null);
            } else {
                List<ProgrammeBatchCourse> matched = programmeBatchCourseRepository.findByMasterCourseId(selectedCourseOrOfferingId);
                if (!matched.isEmpty()) targetOffering = matched.get(0);
            }
        }
        if (targetOffering == null && !assignedOfferings.isEmpty()) {
            targetOffering = assignedOfferings.get(0);
        }

        MasterCourse targetMasterCourse = null;
        if (targetOffering != null && targetOffering.getMasterCourseId() != null) {
            targetMasterCourse = masterCourseRepository.findById(targetOffering.getMasterCourseId()).orElse(null);
        } else if (!finalCourses.isEmpty()) {
            targetMasterCourse = finalCourses.get(0);
        }

        String schoolName = null;
        String deptName = null;
        String progName = null;
        String batchName = null;
        int coCount = 0;
        int poCount = 0;
        int psoCount = 0;
        CourseCoordinatorSetupProgressDto setupProgress = null;

        if (targetOffering != null) {
            String batchId = targetOffering.getBatchId();
            if (batchId != null) {
                poCount = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId).size();
                psoCount = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batchId).size();

                ProgrammeBatch batch = programmeBatchRepository.findById(batchId).orElse(null);
                if (batch != null) {
                    batchName = batch.getName();
                    String progId = batch.getMasterProgrammeId();
                    if (progId != null) {
                        MasterProgramme prog = masterProgrammeRepository.findByIdAndDeletedAtIsNull(progId).orElse(null);
                        if (prog != null) {
                            progName = prog.getName();
                            String deptId = prog.getDepartmentId();
                            if (deptId != null) {
                                Department dept = departmentRepository.findById(deptId).orElse(null);
                                if (dept != null) {
                                    deptName = dept.getName();
                                    String schId = dept.getSchoolId();
                                    if (schId != null) {
                                        School sch = schoolRepository.findById(schId).orElse(null);
                                        if (sch != null) {
                                            schoolName = sch.getName();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            coCount = courseOutcomeRepository.findByProgrammeBatchCourseId(targetOffering.getId()).size();
            setupProgress = getCourseCoordinatorSetupProgress(email, targetOffering.getId());
        } else if (targetMasterCourse != null) {
            setupProgress = getCourseCoordinatorSetupProgress(email, targetMasterCourse.getId());
        }

        if (schoolName == null && scope != null && scope.getSchoolId() != null) {
            School sch = schoolRepository.findById(scope.getSchoolId()).orElse(null);
            if (sch != null) schoolName = sch.getName();
        }

        if (poCount == 0) poCount = 12;
        if (psoCount == 0) psoCount = 2;

        return CourseCoordinatorSummaryDto.builder()
                .schoolName(schoolName)
                .departmentName(deptName)
                .programmeName(progName)
                .batchName(batchName)
                .courseOfferingId(targetOffering != null ? targetOffering.getId() : null)
                .programmeBatchCourseId(targetOffering != null ? targetOffering.getId() : null)
                .courseCode(targetOffering != null ? targetOffering.getCourseCode() : (targetMasterCourse != null ? targetMasterCourse.getCode() : null))
                .courseName(targetOffering != null ? targetOffering.getCourseName() : (targetMasterCourse != null ? targetMasterCourse.getName() : null))
                .coordinatorName(name)
                .coordinatorEmail(email)
                .assignedCourseCount(finalCourses.size())
                .assignedCourses(finalCourses)
                .setupProgress(setupProgress)
                .courseOutcomesCount(coCount)
                .poCount(poCount)
                .psoCount(psoCount)
                .build();
    }

    private String resolveTargetCourseId(String courseId) {
        if (courseId != null && !courseId.isBlank() && masterCourseRepository.existsById(courseId)) {
            return courseId;
        }
        return courseId;
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSetupProgressDto getCourseCoordinatorSetupProgress(String coordinatorEmail, String courseId) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] getCourseCoordinatorSetupProgress called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId);
        if (targetCourseId != null && !targetCourseId.isBlank()) {
            if (programmeBatchCourseRepository.existsById(targetCourseId)) {
                enforceProgrammeBatchCourseScope(targetCourseId);
            } else if (masterCourseRepository.existsById(targetCourseId)) {
                enforceCourseScope(targetCourseId);
            }
        }
        CourseCoordinatorSetupProgress progress = (targetCourseId != null)
                ? ccSetupProgressRepository.findByProgrammeBatchCourseId(targetCourseId).orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + targetCourseId)
                        .programmeBatchCourseId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .currentStep(1)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("cos,co_mapping,direct,indirect,attainment,course_atr")
                        .updatedAt(ZonedDateTime.now())
                        .build())
                : null;
        if (progress == null) {
            return null;
        }

        List<String> completed = (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank())
                ? Arrays.asList(progress.getCompletedSteps().split(","))
                : Collections.emptyList();
        List<String> pending = (progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank())
                ? Arrays.asList(progress.getPendingSteps().split(","))
                : Collections.emptyList();

        return CourseCoordinatorSetupProgressDto.builder()
                .id(progress.getId())
                .courseId(progress.getCourseId())
                .currentStep(progress.getCurrentStep())
                .completedSteps(completed)
                .pendingSteps(pending)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto updateCourseCoordinatorSetupProgress(String coordinatorEmail, String courseId, Integer currentStep) {
        return updateCourseCoordinatorSetupProgress(coordinatorEmail, courseId, currentStep, null);
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto updateCourseCoordinatorSetupProgress(
            String coordinatorEmail,
            String courseId,
            Integer currentStep,
            Map<String, Object> body) {
        String effectiveCourseId = courseId;
        Integer effectiveStep = currentStep;
        String effectiveEmail = coordinatorEmail;
        List<String> completedStepsList = null;
        List<String> pendingStepsList = null;

        if (body != null) {
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("courseId")) {
                effectiveCourseId = String.valueOf(body.get("courseId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("offeringId")) {
                effectiveCourseId = String.valueOf(body.get("offeringId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("courseOfferingId")) {
                effectiveCourseId = String.valueOf(body.get("courseOfferingId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("programmeBatchCourseId")) {
                effectiveCourseId = String.valueOf(body.get("programmeBatchCourseId"));
            }
            if ((effectiveEmail == null || effectiveEmail.isBlank()) && body.containsKey("coordinatorEmail")) {
                effectiveEmail = String.valueOf(body.get("coordinatorEmail"));
            }
            if ((effectiveEmail == null || effectiveEmail.isBlank()) && body.containsKey("email")) {
                effectiveEmail = String.valueOf(body.get("email"));
            }
            if (body.containsKey("stepNumber")) {
                try { effectiveStep = Integer.parseInt(String.valueOf(body.get("stepNumber"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("currentStep")) {
                try { effectiveStep = Integer.parseInt(String.valueOf(body.get("currentStep"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("step")) {
                try { effectiveStep = Integer.parseInt(String.valueOf(body.get("step"))); } catch (Exception ignored) {}
            }
            if (body.get("completedSteps") instanceof List<?> list) {
                completedStepsList = list.stream().map(String::valueOf).toList();
            } else if (body.containsKey("completedSteps") && body.get("completedSteps") != null) {
                completedStepsList = Arrays.asList(String.valueOf(body.get("completedSteps")).split(","));
            }
            if (body.get("pendingSteps") instanceof List<?> list) {
                pendingStepsList = list.stream().map(String::valueOf).toList();
            } else if (body.containsKey("pendingSteps") && body.get("pendingSteps") != null) {
                pendingStepsList = Arrays.asList(String.valueOf(body.get("pendingSteps")).split(","));
            }
        }

        if (effectiveCourseId == null || effectiveCourseId.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseId is required for setup progress.");
        }
        String targetCourseId = effectiveCourseId.trim();

        System.out.println("[AcademicService] updateCourseCoordinatorSetupProgress called | courseId: " + targetCourseId + " | stepNumber: " + effectiveStep);
        if (programmeBatchCourseRepository.existsById(targetCourseId)) {
            enforceProgrammeBatchCourseScope(targetCourseId);
        } else if (masterCourseRepository.existsById(targetCourseId)) {
            enforceCourseScope(targetCourseId);
        }

        final String finalEmail = effectiveEmail;
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByProgrammeBatchCourseId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeBatchCourseId(targetCourseId)
                        .coordinatorEmail(finalEmail)
                        .currentStep(1)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("cos,co_mapping,direct,indirect,attainment,course_atr")
                        .build());

        if (effectiveStep != null) {
            progress.setCurrentStep(effectiveStep);
        }
        if (effectiveEmail != null && !effectiveEmail.isBlank()) {
            progress.setCoordinatorEmail(effectiveEmail);
        }
        if (completedStepsList != null) {
            progress.setCompletedSteps(String.join(",", completedStepsList));
        }
        if (pendingStepsList != null) {
            progress.setPendingSteps(String.join(",", pendingStepsList));
        }
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(effectiveEmail, targetCourseId);
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto completeCourseCoordinatorSetup(String coordinatorEmail, String courseId) {
        return completeCourseCoordinatorSetup(coordinatorEmail, courseId, null);
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto completeCourseCoordinatorSetup(
            String coordinatorEmail,
            String courseId,
            Map<String, Object> body) {
        String effectiveCourseId = courseId;
        String effectiveEmail = coordinatorEmail;

        if (body != null) {
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("courseId")) {
                effectiveCourseId = String.valueOf(body.get("courseId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("offeringId")) {
                effectiveCourseId = String.valueOf(body.get("offeringId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("courseOfferingId")) {
                effectiveCourseId = String.valueOf(body.get("courseOfferingId"));
            }
            if ((effectiveCourseId == null || effectiveCourseId.isBlank()) && body.containsKey("programmeBatchCourseId")) {
                effectiveCourseId = String.valueOf(body.get("programmeBatchCourseId"));
            }
            if ((effectiveEmail == null || effectiveEmail.isBlank()) && body.containsKey("coordinatorEmail")) {
                effectiveEmail = String.valueOf(body.get("coordinatorEmail"));
            }
            if ((effectiveEmail == null || effectiveEmail.isBlank()) && body.containsKey("email")) {
                effectiveEmail = String.valueOf(body.get("email"));
            }
        }

        if (effectiveCourseId == null || effectiveCourseId.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseId is required to complete setup progress.");
        }
        String targetCourseId = effectiveCourseId.trim();

        System.out.println("[AcademicService] completeCourseCoordinatorSetup called | courseId: " + targetCourseId);
        if (programmeBatchCourseRepository.existsById(targetCourseId)) {
            enforceProgrammeBatchCourseScope(targetCourseId);
            enforceCourseCoordinatorScope(targetCourseId);
        } else if (masterCourseRepository.existsById(targetCourseId)) {
            enforceCourseScope(targetCourseId);
            enforceCourseCoordinatorScope(targetCourseId);
        }

        final String finalCompleteEmail = effectiveEmail;
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByProgrammeBatchCourseId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeBatchCourseId(targetCourseId)
                        .coordinatorEmail(finalCompleteEmail)
                        .build());

        progress.setOverallStatus(SetupStepStatus.COMPLETED);
        progress.setCompletedSteps("cos,co_mapping,direct,indirect,attainment,course_atr");
        progress.setPendingSteps("");
        if (effectiveEmail != null && !effectiveEmail.isBlank()) {
            progress.setCoordinatorEmail(effectiveEmail);
        }
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(effectiveEmail, targetCourseId);
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<MasterProgramme> getAllProgrammes() {
        System.out.println("[AcademicService] getAllProgrammes called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            return getProgrammesBySchool(scope.getRequiredSchoolId());
        }
        if (scope != null && scope.isHod()) {
            List<Department> hodDepts = (scope.getEmail() != null && !scope.getEmail().isBlank())
                    ? departmentRepository.findByHodEmailIgnoreCase(scope.getEmail().trim())
                    : Collections.emptyList();
            if (hodDepts != null && !hodDepts.isEmpty()) {
                List<String> deptIds = hodDepts.stream().map(Department::getId).toList();
                List<MasterProgramme> list = masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds);
                list.forEach(this::enrichProgrammeCoordinator);
                return list;
            }
            return getProgrammesByDepartment(scope.getRequiredDepartmentId());
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            Set<String> progIds = new LinkedHashSet<>();
            if (scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(scope.getEmail().trim());
                if (batches != null) {
                    batches.stream()
                            .map(ProgrammeBatch::getMasterProgrammeId)
                            .filter(id -> id != null && !id.isBlank())
                            .forEach(progIds::add);
                }
            }
            if (scope.getProgrammeId() != null && !scope.getProgrammeId().isBlank()) {
                progIds.add(scope.getProgrammeId());
            }

            if (!progIds.isEmpty()) {
                List<MasterProgramme> progs = masterProgrammeRepository.findByIdInAndDeletedAtIsNull(progIds);
                progs.forEach(this::enrichProgrammeCoordinator);
                return progs;
            }

            if (scope.getProgrammeId() != null) {
                MasterProgramme p = masterProgrammeRepository.findByIdAndDeletedAtIsNull(scope.getProgrammeId()).orElse(null);
                if (p != null) {
                    enrichProgrammeCoordinator(p);
                    return List.of(p);
                }
            }
            return Collections.emptyList();
        }
        List<MasterProgramme> list = masterProgrammeRepository.findByDeletedAtIsNull();
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched all programmes (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public MasterProgramme getProgrammeById(String id) {
        System.out.println("[AcademicService] getProgrammeById called | id: " + id);
        if (id == null || id.isBlank()) return null;
        MasterProgramme p = masterProgrammeRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (p == null) return null;
        enforceProgrammeScope(p.getId());
        enrichProgrammeCoordinator(p);
        return p;
    }

    @Transactional(readOnly = true)
    public List<MasterProgramme> getProgrammesByCoordinatorEmail(String coordinatorEmail) {
        System.out.println("[AcademicService] getProgrammesByCoordinatorEmail called | coordinatorEmail: " + coordinatorEmail);
        CurrentUserScope scope = getScope();
        String effectiveEmail = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? coordinatorEmail.trim().toLowerCase()
                : (scope != null && scope.getEmail() != null ? scope.getEmail().trim().toLowerCase() : null);

        Set<String> programmeIds = new LinkedHashSet<>();
        if (effectiveEmail != null && !effectiveEmail.isBlank()) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(effectiveEmail);
            if (batches != null) {
                batches.stream()
                        .map(ProgrammeBatch::getMasterProgrammeId)
                        .filter(id -> id != null && !id.isBlank())
                        .forEach(programmeIds::add);
            }
            userRepository.findByEmail(effectiveEmail).ifPresent(u -> {
                if (u.getProgrammeId() != null && !u.getProgrammeId().isBlank()) {
                    programmeIds.add(u.getProgrammeId());
                }
            });
        }

        if (scope != null && scope.getProgrammeId() != null && !scope.getProgrammeId().isBlank()) {
            programmeIds.add(scope.getProgrammeId());
        }

        if (!programmeIds.isEmpty()) {
            List<MasterProgramme> programmes = masterProgrammeRepository.findByIdInAndDeletedAtIsNull(programmeIds);
            programmes.forEach(this::enrichProgrammeCoordinator);
            System.out.println("[AcademicService] Found " + programmes.size() + " unique master-programmes for coordinatorEmail: " + effectiveEmail);
            return programmes;
        }

        List<MasterProgramme> all = getAllProgrammes();
        return all;
    }

    @Transactional(readOnly = true)
    public List<MasterProgramme> getProgrammesBySchool(String schoolId) {
        System.out.println("[AcademicService] getProgrammesBySchool called | schoolId: " + schoolId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            String dirSchoolId = scope.getRequiredSchoolId();
            if (schoolId != null && !schoolId.isBlank() && !schoolId.equals(dirSchoolId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot view programmes of a different school.");
            }
            schoolId = dirSchoolId;
        }
        if (scope != null && scope.isHod()) {
            return getAllProgrammes();
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            enforceSchoolScope(schoolId);
            return getAllProgrammes();
        }
        List<Department> depts = departmentRepository.findBySchoolId(schoolId);
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        List<MasterProgramme> list = masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional(readOnly = true)
    public List<MasterProgramme> getProgrammesByDepartment(String departmentId) {
        System.out.println("[AcademicService] getProgrammesByDepartment called | departmentId: " + departmentId);
        CurrentUserScope scope = getScope();
        if (departmentId == null || departmentId.isBlank()) {
            return getAllProgrammes();
        }
        enforceDepartmentScope(departmentId);
        if (scope != null && scope.isProgrammeCoordinator()) {
            return getAllProgrammes();
        }
        List<MasterProgramme> list = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for departmentId: " + departmentId);
        return list;
    }

    @Transactional
    public MasterProgramme saveProgramme(MasterProgramme programme) {
        System.out.println("[AcademicService] saveMasterProgramme called | id: " + (programme != null ? programme.getId() : "null") + " | name: " + (programme != null ? programme.getName() : "null") + " | coordinator: " + (programme != null ? programme.getCoordinator() : "null") + " | coordinatorEmail: " + (programme != null ? programme.getCoordinatorEmail() : "null"));
        if (programme == null) return null;

        if (programme.getId() != null) {
            MasterProgramme existing = masterProgrammeRepository.findByIdAndDeletedAtIsNull(programme.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getId());
            }
        }
        if (programme.getDepartmentId() != null) {
            enforceDepartmentScope(programme.getDepartmentId());
        }

        MasterProgramme targetProg = programme;
        if (programme.getId() != null) {
            Optional<MasterProgramme> existingOpt = masterProgrammeRepository.findByIdAndDeletedAtIsNull(programme.getId());
            if (existingOpt.isPresent()) {
                MasterProgramme existing = existingOpt.get();
                if (programme.getName() != null) existing.setName(programme.getName());
                if (programme.getCode() != null) existing.setCode(programme.getCode());
                if (programme.getDepartmentId() != null) existing.setDepartmentId(programme.getDepartmentId());
                if (programme.getDurationYears() != null) existing.setDurationYears(programme.getDurationYears());
                if (programme.getStatus() != null) existing.setStatus(programme.getStatus());
                if (programme.getDepartmentName() != null) existing.setDepartmentName(programme.getDepartmentName());
                if (programme.getCoordinator() != null) existing.setCoordinator(programme.getCoordinator());
                if (programme.getCoordinatorEmail() != null) existing.setCoordinatorEmail(programme.getCoordinatorEmail());
                targetProg = existing;
            }
        } else {
            targetProg.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        }
        
        // Ensure department is loaded to get schoolId
        String deptId = targetProg.getDepartmentId();
        if (deptId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department ID is required.");
        }
        Department dept = departmentRepository.findById(deptId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Department ID."));
        String schoolId = dept.getSchoolId();
        
        String excludeId = targetProg.getId();
        
        if (targetProg.getCode() != null) {
            boolean codeExists = masterProgrammeRepository.existsByCodeInSchoolExcludeId(schoolId, targetProg.getCode(), excludeId);
            if (codeExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Programme code already exists in this school.");
            }
        }
        
        if (targetProg.getName() != null) {
            boolean nameExists = masterProgrammeRepository.existsByNameInSchoolExcludeId(schoolId, targetProg.getName(), excludeId);
            if (nameExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Programme name already exists in this school.");
            }
        }

        if (programme.getCoordinator() != null && !programme.getCoordinator().isBlank()) {
            targetProg.setCoordinator(programme.getCoordinator());
        }
        if (programme.getCoordinatorEmail() != null && !programme.getCoordinatorEmail().isBlank()) {
            targetProg.setCoordinatorEmail(programme.getCoordinatorEmail());
        }

        final MasterProgramme finalProg = targetProg;
        enrichProgrammeCoordinator(finalProg);

        // Populate department name if missing
        if ((finalProg.getDepartmentName() == null || finalProg.getDepartmentName().isBlank()) && finalProg.getDepartmentId() != null) {
            departmentRepository.findById(finalProg.getDepartmentId()).ifPresent(d -> finalProg.setDepartmentName(d.getName()));
        }

        // Bidirectionally synchronize user record if coordinator assigned
        String coordEmail = finalProg.getCoordinatorEmail();
        if (coordEmail != null && !coordEmail.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(coordEmail.trim());
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(coordEmail.trim());
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setProgrammeId(finalProg.getId());
                user.setProgramme(finalProg.getName());
                if (finalProg.getDepartmentId() != null) {
                    user.setDepartmentId(finalProg.getDepartmentId());
                }
                if (user.getRole() == UserRole.FACULTY) {
                    user.setRole(UserRole.PROGRAMME_COORDINATOR);
                }
                userRepository.save(user);
                System.out.println("[AcademicService] Synchronized user " + user.getEmail() + " as PC for programme " + finalProg.getName());
            }
        }

        boolean isNewProg = (finalProg.getId() == null || !masterProgrammeRepository.existsByIdAndDeletedAtIsNull(finalProg.getId()));
        MasterProgramme saved = masterProgrammeRepository.save(finalProg);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNewProg ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.MASTER_PROGRAMME, saved.getId(), null, "ACTIVE", isNewProg ? "Created MasterProgramme" : "Updated MasterProgramme", java.util.Map.of("code", saved.getCode() != null ? saved.getCode() : "", "name", saved.getName() != null ? saved.getName() : ""));
        }
        System.out.println("[AcademicService] Saved programme with id: " + saved.getId() + ", coordinator: " + saved.getCoordinator() + ", coordinatorEmail: " + saved.getCoordinatorEmail());
        return saved;
    }

    @Transactional
    public void deleteProgramme(String id) {
        System.out.println("[AcademicService] deleteMasterProgramme called | id: " + id);
        enforceProgrammeScope(id);
        MasterProgramme existing = masterProgrammeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MasterProgramme not found: " + id));
        existing.setStatus("DELETED");
        existing.setDeletedAt(ZonedDateTime.now());
        
        CurrentUserScope scope = getScope();
        String deletedBy = (scope != null && scope.getEmail() != null) ? scope.getEmail() : "system";
        existing.setDeletedBy(deletedBy);
        
        masterProgrammeRepository.save(existing);
        
        // Soft delete all active child batches
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(id);
        for (ProgrammeBatch batch : batches) {
            if (batch.getDeletedAt() == null) {
                batch.setStatus("DELETED");
                batch.setDeletedAt(ZonedDateTime.now());
                batch.setDeletedBy(deletedBy);
                programmeBatchRepository.save(batch);
                // Child courses of batches are handled by filtering on active batches in queries, or we can soft delete them.
                // Let's soft delete courses as well.
                List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchId(batch.getId());
                for (ProgrammeBatchCourse course : courses) {
                    if (course.getDeletedAt() == null) {
                        course.setStatus("DELETED");
                        course.setDeletedAt(ZonedDateTime.now());
                        course.setDeletedBy(deletedBy);
                        programmeBatchCourseRepository.save(course);
                    }
                }
            }
        }
        
        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    com.dypiu.nba.audit.AuditAction.DELETE,
                    com.dypiu.nba.audit.ResourceType.MASTER_PROGRAMME,
                    id,
                    null,
                    "DELETED",
                    "Soft-deleted MasterProgramme and its active batches/courses",
                    java.util.Map.of("code", existing.getCode() != null ? existing.getCode() : "")
            );
        }
        System.out.println("[AcademicService] Soft-deleted programme with id: " + id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<ProgrammeBatch> getAllBatches() {
        System.out.println("[AcademicService] getAllBatches called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> deptIds = depts.stream().map(Department::getId).toList();
            List<MasterProgramme> progs = deptIds.isEmpty() ? Collections.emptyList() : masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds);
            List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isHod()) {
            List<MasterProgramme> progs = getAllProgrammes();
            List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            if (scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<ProgrammeBatch> pcBatches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(scope.getEmail().trim());
                if (pcBatches != null && !pcBatches.isEmpty()) {
                    return pcBatches;
                }
            }
            if (scope.getProgrammeId() != null && !scope.getProgrammeId().isBlank()) {
                return programmeBatchRepository.findByMasterProgrammeId(scope.getProgrammeId());
            }
            return Collections.emptyList();
        }
        if (scope != null && scope.isFaculty()) {
            List<ProgrammeBatchCourse> allOfferings = programmeBatchCourseRepository.findAll();
            Set<String> assignedBatchIds = allOfferings.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .map(ProgrammeBatchCourse::getBatchId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return assignedBatchIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findAllById(assignedBatchIds);
        }
        List<ProgrammeBatch> list = programmeBatchRepository.findAll();
        System.out.println("[AcademicService] Fetched all batches (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatch> getBatchesByCoordinatorEmailAndProgramme(String coordinatorEmail, String masterProgrammeId) {
        CurrentUserScope scope = getScope();
        String effectiveEmail = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? coordinatorEmail.trim().toLowerCase()
                : (scope != null && scope.getEmail() != null ? scope.getEmail().trim().toLowerCase() : null);

        List<ProgrammeBatch> batches;
        if (effectiveEmail != null && !effectiveEmail.isBlank()) {
            batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(effectiveEmail);
        } else if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            enforceProgrammeScope(masterProgrammeId);
            return programmeBatchRepository.findByMasterProgrammeId(masterProgrammeId.trim());
        } else {
            return getAllBatches();
        }

        if (masterProgrammeId != null && !masterProgrammeId.isBlank()) {
            String progIdTrim = masterProgrammeId.trim();
            batches = batches.stream()
                    .filter(b -> progIdTrim.equals(b.getMasterProgrammeId()))
                    .collect(Collectors.toList());
        }
        return batches;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatchCourse> getProgrammeBatchCoursesByCoordinatorEmail(String coordinatorEmail, String batchId) {
        System.out.println("[AcademicService] getProgrammeBatchCoursesByCoordinatorEmail called | coordinatorEmail: " + coordinatorEmail + " | batchId: " + batchId);
        CurrentUserScope scope = getScope();
        String effectiveEmail = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? coordinatorEmail.trim().toLowerCase()
                : (scope != null && scope.getEmail() != null ? scope.getEmail().trim().toLowerCase() : null);

        User user = null;
        if (effectiveEmail != null) {
            user = userRepository.findByEmailIgnoreCase(effectiveEmail)
                    .or(() -> userRepository.findByUsernameIgnoreCase(effectiveEmail))
                    .orElse(null);
        }

        Long userId = user != null ? user.getId() : (scope != null ? scope.getUserId() : null);
        String userName = user != null ? user.getName() : (scope != null ? scope.getName() : null);

        List<ProgrammeBatchCourse> list = (batchId != null && !batchId.isBlank())
                ? programmeBatchCourseRepository.findByProgrammeBatchId(batchId.trim())
                : programmeBatchCourseRepository.findAll();

        List<ProgrammeBatchCourse> filtered = list.stream()
                .filter(o -> {
                    if (userId != null && o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), userId)) {
                        return true;
                    }
                    if (userName != null && o.getCourseCoordinatorName() != null && o.getCourseCoordinatorName().equalsIgnoreCase(userName)) {
                        return true;
                    }
                    if (effectiveEmail != null && o.getAssignedFaculty() != null && o.getAssignedFaculty().toLowerCase().contains(effectiveEmail)) {
                        return true;
                    }
                    if (userName != null && o.getAssignedFaculty() != null && o.getAssignedFaculty().toLowerCase().contains(userName.toLowerCase())) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        filtered.forEach(this::enrichOffering);
        return filtered;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatch> getBatchesByCourseCoordinatorEmail(String courseCoordinatorEmail) {
        System.out.println("[AcademicService] getBatchesByCourseCoordinatorEmail called | courseCoordinatorEmail: " + courseCoordinatorEmail);
        List<ProgrammeBatchCourse> assignedCourses = getProgrammeBatchCoursesByCoordinatorEmail(courseCoordinatorEmail, null);
        if (assignedCourses.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> batchIds = assignedCourses.stream()
                .map(ProgrammeBatchCourse::getProgrammeBatchId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (batchIds.isEmpty()) {
            return Collections.emptyList();
        }
        return programmeBatchRepository.findAllById(batchIds);
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatch> getBatchesByProgramme(String programmeId) {
        System.out.println("[AcademicService] getBatchesByMasterProgramme called | programmeId: " + programmeId);
        enforceProgrammeScope(programmeId);
        List<ProgrammeBatch> list = programmeBatchRepository.findByMasterProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched batches (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional(readOnly = true)
    public ProgrammeBatch getBatchById(String id) {
        System.out.println("[AcademicService] getBatchById called | id: " + id);
        if (id == null || id.isBlank()) return null;
        ProgrammeBatch batch = programmeBatchRepository.findById(id).orElse(null);
        if (batch == null) return null;
        enforceBatchScope(batch.getId());
        return batch;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatch> getBatchesScoped(String programmeId, String userEmail, String role) {
        System.out.println("================================================================================");
        System.out.println("[AcademicService] >>> getBatchesScoped | programmeId: " + programmeId + " | userEmail: " + userEmail + " | role: " + role);

        CurrentUserScope scope = getScope();

        // 1. Explicit programme filter
        if (programmeId != null && !programmeId.isBlank()) {
            return getBatchesByProgramme(programmeId);
        }

        // 2. Director scope
        if (scope != null && scope.isDirector()) {
            return getAllBatches();
        }

        // 3. HOD scope
        if (scope != null && scope.isHod()) {
            return getAllBatches();
        }

        // 4. PC scope
        if (scope != null && scope.isProgrammeCoordinator()) {
            return getAllBatches();
        }

        // 5. Admin / Global roles
        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            return programmeBatchRepository.findAll();
        }

        // 6. Fallback for other callers (MasterCourse Coordinator/Faculty)
        return getAllBatches();
    }

    @Transactional
    public ProgrammeBatch saveBatch(ProgrammeBatch batch) {
        System.out.println("[AcademicService] saveProgrammeBatch called | name: " + (batch != null ? batch.getName() : "null"));
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProgrammeBatch details cannot be null.");
        }
        if (batch.getId() != null) {
            ProgrammeBatch existing = programmeBatchRepository.findById(batch.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getProgrammeId());
                batchLifecycleService.enforceBatchEditability(batch.getId());
            }
        }
        if (batch.getProgrammeId() != null) {
            enforceProgrammeScope(batch.getProgrammeId());
        }
        if (batch.getStartYear() != null && batch.getEndYear() != null) {
            if (batch.getEndYear() <= batch.getStartYear()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endYear (" + batch.getEndYear() + ") must be greater than startYear (" + batch.getStartYear() + ").");
            }
            batch.setDurationYears(batch.getEndYear() - batch.getStartYear());
        }
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        boolean isNewBatch = (batch.getId() == null || !programmeBatchRepository.existsById(batch.getId()));
        ProgrammeBatch saved = programmeBatchRepository.save(batch);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNewBatch ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.PROGRAMME_BATCH, saved.getId(), null, "ACTIVE", isNewBatch ? "Created ProgrammeBatch" : "Updated ProgrammeBatch", java.util.Map.of("name", saved.getName() != null ? saved.getName() : ""));
        }
        System.out.println("[AcademicService] Saved batch with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteBatch(String id) {
        System.out.println("[AcademicService] deleteProgrammeBatch called | id: " + id);
        ProgrammeBatch batch = programmeBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found with id: " + id));
        enforceProgrammeScope(batch.getProgrammeId());
        programmeBatchRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted batch with id: " + id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<MasterCourse> getAllCourses() {
        System.out.println("[AcademicService] getAllCourses called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> deptIds = depts.stream().map(Department::getId).toList();
            List<MasterProgramme> progs = deptIds.isEmpty() ? Collections.emptyList() : masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds);
            List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : masterCourseRepository.findByMasterProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isHod()) {
            List<MasterProgramme> progs = getAllProgrammes();
            List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : masterCourseRepository.findByMasterProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            List<MasterProgramme> progs = getAllProgrammes();
            List<String> progIds = progs.stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : masterCourseRepository.findByMasterProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isFaculty()) {
            List<ProgrammeBatchCourse> allOfferings = programmeBatchCourseRepository.findAll();
            Set<String> assignedCourseIds = allOfferings.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .map(ProgrammeBatchCourse::getCourseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return assignedCourseIds.isEmpty() ? Collections.emptyList() : masterCourseRepository.findAllById(assignedCourseIds);
        }
        List<MasterCourse> list = masterCourseRepository.findAll();
        System.out.println("[AcademicService] Fetched all courses (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public MasterCourse getCourseById(String id) {
        System.out.println("[AcademicService] getCourseById called | id: " + id);
        MasterCourse course = masterCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MasterCourse not found with id: " + id));
        enforceCourseScope(id);
        return course;
    }

    @Transactional(readOnly = true)
    public List<MasterCourse> getCoursesByProgramme(String programmeId, String batchId) {
        System.out.println("[AcademicService] getCoursesByMasterProgramme called | programmeId: " + programmeId + " | batchId: " + batchId);
        enforceProgrammeScope(programmeId);
        List<MasterCourse> list = masterCourseRepository.findByMasterProgrammeId(programmeId);
        
        if (batchId != null && !batchId.isBlank()) {
            for (MasterCourse course : list) {
                List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(course.getId());
                ProgrammeBatchCourse targetOffering = offerings.stream()
                        .filter(o -> batchId.equals(o.getProgrammeBatchId()))
                        .findFirst()
                        .orElse(null);
                
                if (targetOffering != null) {
                    course.setSemester(targetOffering.getSemester() != null ? String.valueOf(targetOffering.getSemester()) : null);
                    course.setCoordinator(targetOffering.getCourseCoordinatorName());
                    course.setFaculty(targetOffering.getCourseCoordinatorName());
                    course.setAssignedFaculty(targetOffering.getAssignedFaculty());
                    course.setProgrammeBatchCourseId(targetOffering.getId());
                    
                    // We also need coordinatorEmail if we can extract it from assignedFaculty
                    if (targetOffering.getAssignedFaculty() != null && targetOffering.getAssignedFaculty().contains("(")) {
                        String emailPart = targetOffering.getAssignedFaculty().substring(targetOffering.getAssignedFaculty().indexOf("(") + 1, targetOffering.getAssignedFaculty().indexOf(")"));
                        course.setCoordinatorEmail(emailPart);
                    }
                }
            }
        }
        
        System.out.println("[AcademicService] Fetched courses (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public MasterCourse saveCourse(MasterCourse course) {
        System.out.println("[AcademicService] saveMasterCourse called | name: " + (course != null ? course.getName() : "null"));
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterCourse details cannot be null.");
        }
        if (course.getId() != null) {
            MasterCourse existing = masterCourseRepository.findById(course.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getProgrammeId());
            }
        }
        if (course.getProgrammeId() != null) {
            enforceProgrammeScope(course.getProgrammeId());
        }
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        boolean isNewCourse = (course.getId() == null || !masterCourseRepository.existsById(course.getId()));
        MasterCourse saved = masterCourseRepository.save(course);
        if (auditLogService != null) {
            auditLogService.recordSuccess(isNewCourse ? com.dypiu.nba.audit.AuditAction.CREATE : com.dypiu.nba.audit.AuditAction.UPDATE, com.dypiu.nba.audit.ResourceType.MASTER_COURSE, saved.getId(), null, "ACTIVE", isNewCourse ? "Created MasterCourse" : "Updated MasterCourse", java.util.Map.of("code", saved.getCode() != null ? saved.getCode() : "", "name", saved.getName() != null ? saved.getName() : ""));
        }
        System.out.println("[AcademicService] Saved course with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteCourse(String id) {
        System.out.println("[AcademicService] deleteMasterCourse called | id: " + id);
        MasterCourse course = masterCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MasterCourse not found with id: " + id));
        enforceProgrammeScope(course.getProgrammeId());
        masterCourseRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted course with id: " + id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        System.out.println("[AcademicService] getStudentsByProgrammeBatch called | batchId: " + batchId);
        if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId);
        }
        List<Student> list = studentRepository.findByProgrammeBatchId(batchId);
        System.out.println("[AcademicService] Fetched students (" + list.size() + " items) for batchId: " + batchId);
        return list;
    }

    @Transactional
    public Student saveStudent(Student student) {
        System.out.println("[AcademicService] saveStudent called | name: " + (student != null ? student.getName() : "null"));
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student details cannot be null.");
        }
        if (student.getBatchId() != null) {
            enforceBatchScope(student.getBatchId());
        }
        if (student.getId() != null) {
            Student existing = studentRepository.findById(student.getId()).orElse(null);
            if (existing != null && existing.getBatchId() != null) {
                enforceBatchScope(existing.getBatchId());
            }
        }
        if (student.getId() == null) student.setId("std-" + UUID.randomUUID().toString().substring(0, 8));
        Student saved = studentRepository.save(student);
        System.out.println("[AcademicService] Saved student with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteStudent(String id) {
        System.out.println("[AcademicService] deleteStudent called | id: " + id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        if (student.getBatchId() != null) {
            enforceBatchScope(student.getBatchId());
        }
        studentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted student with id: " + id);
    }

    // --- HOD Department Summary ---
    @Transactional(readOnly = true)
    public HodDepartmentSummaryDto getHodDepartmentSummary(String hodEmail) {
        return getHodDepartmentSummary(null, hodEmail);
    }

    @Transactional(readOnly = true)
    public HodDepartmentSummaryDto getHodDepartmentSummary(String departmentId, String hodEmail) {
        System.out.println("[AcademicService] getHodDepartmentSummary called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);
        CurrentUserScope scope = getScope();
        if (departmentId != null && !departmentId.isBlank() && !departmentId.equals("dept-1")) {
            enforceDepartmentScope(departmentId.trim());
        }

        String targetDeptId = null;

        if (scope != null && scope.isHod()) {
            targetDeptId = scope.getRequiredDepartmentId();
        } else if (departmentId != null && !departmentId.isBlank()) {
            targetDeptId = departmentId.trim();
        } else if (hodEmail != null && !hodEmail.isBlank()) {
            List<Department> deptList = departmentRepository.findByHodEmailIgnoreCase(hodEmail.trim());
            if (!deptList.isEmpty()) {
                targetDeptId = deptList.get(0).getId();
            }
        } else if (scope != null && scope.getDepartmentId() != null) {
            targetDeptId = scope.getDepartmentId();
        }

        if (targetDeptId == null || targetDeptId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department scope cannot be determined.");
        }

        enforceDepartmentScope(targetDeptId);

        final String finalDeptId = targetDeptId;
        Department dept = departmentRepository.findById(finalDeptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + finalDeptId));

        enforceSchoolScope(dept.getSchoolId());

        String deptId = dept.getId();
        String deptName = dept.getName();
        String deptCode = dept.getCode();
        String resolvedHodName = dept.getHod();
        String resolvedHodEmail = (dept.getHodEmail() != null && !dept.getHodEmail().isBlank()) ? dept.getHodEmail() : hodEmail;
        String schoolId = dept.getSchoolId();

        // School info
        String schoolName = "School of Engineering and Technology";
        if (schoolId != null) {
            Optional<School> schOpt = schoolRepository.findById(schoolId);
            if (schOpt.isPresent()) {
                schoolName = schOpt.get().getName();
            }
        }

        // Programmes under department
        List<MasterProgramme> programmes = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(deptId);
        int programmeCount = programmes.size();

        // Count assigned coordinators
        int assignedCoordinatorsCount = (int) programmes.stream()
                .filter(p -> (p.getCoordinator() != null && !p.getCoordinator().isBlank() && !"Unassigned".equalsIgnoreCase(p.getCoordinator()) && !"No coordinator assigned yet".equalsIgnoreCase(p.getCoordinator()) && !"Pending HOD Assignment".equalsIgnoreCase(p.getCoordinator())) || (p.getCoordinatorEmail() != null && !p.getCoordinatorEmail().isBlank()))
                .count();

        // Courses under department's programmes
        List<String> progIds = programmes.stream().map(MasterProgramme::getId).toList();
        int courseCount = 0;
        if (!progIds.isEmpty()) {
            courseCount = masterCourseRepository.findByMasterProgrammeIdIn(progIds).size();
        }

        HodSetupProgressDto progressDto = getHodSetupProgress(deptId, resolvedHodEmail);
        System.out.println("[AcademicService] Fetched HOD department summary for deptId: " + deptId + " (" + deptName + ") | hodEmail: " + resolvedHodEmail);

        return HodDepartmentSummaryDto.builder()
                .deptId(deptId)
                .deptCode(deptCode)
                .deptName(deptName)
                .hodName(resolvedHodName)
                .hodEmail(resolvedHodEmail)
                .schoolId(schoolId)
                .schoolName(schoolName)
                .programmeCount(programmeCount)
                .assignedCoordinatorsCount(assignedCoordinatorsCount)
                .courseCount(courseCount)
                .setupProgress(progressDto)
                .build();
    }

    private String resolveTargetDeptId(String departmentId, String hodEmail) {
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isHod()) {
            return scope.getRequiredDepartmentId();
        }
        if (departmentId != null && !departmentId.isBlank() && !departmentId.contains("@") && !departmentId.equals("null")) {
            return departmentId;
        }

        String search = (hodEmail != null && !hodEmail.isBlank() && !hodEmail.equals("null"))
                ? hodEmail.trim()
                : (departmentId != null && !departmentId.equals("null") ? departmentId.trim() : null);

        if (search != null && !search.isBlank()) {
            List<Department> deptList = departmentRepository.findByHodEmailIgnoreCase(search);
            if (!deptList.isEmpty()) {
                return deptList.get(0).getId();
            }
        }
        if (scope != null && scope.getDepartmentId() != null) {
            return scope.getDepartmentId();
        }
        return departmentId;
    }

    // --- HOD Setup Progress ---
    @Transactional(readOnly = true)
    public HodSetupProgressDto getHodSetupProgress(
            String departmentId,
            String hodEmail) {
        System.out.println("[AcademicService] getHodSetupProgress called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);
        if (departmentId != null && !departmentId.isBlank() && !departmentId.equals("dept-1")) {
            enforceDepartmentScope(departmentId.trim());
        }

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        if (targetDeptId == null || targetDeptId.isBlank()) {
            return null;
        }
        enforceDepartmentScope(targetDeptId);

        final String finalDeptId = targetDeptId;
        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(finalDeptId)
                .orElseGet(() -> createDefaultProgress(finalDeptId, hodEmail));

        return buildHodSetupProgressDto(progress);
    }

    @Transactional
    public HodSetupProgressDto updateHodSetupProgress(
            String departmentId,
            Integer stepNumber,
            String hodEmail) {
        return updateHodSetupProgress(departmentId, stepNumber, null, null, hodEmail);
    }

    @Transactional
    public HodSetupProgressDto updateHodSetupProgress(
            String departmentId,
            Integer targetStep,
            String completedStep,
            List<String> completedStepsList,
            String hodEmail) {
        System.out.println("[AcademicService] updateHodSetupProgress called | departmentId: " + departmentId + " | targetStep: " + targetStep + " | completedStep: " + completedStep + " | completedStepsList: " + completedStepsList + " | hodEmail: " + hodEmail);
        if (departmentId != null && !departmentId.isBlank() && !departmentId.equals("dept-1")) {
            enforceDepartmentScope(departmentId.trim());
        }

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        if (targetDeptId == null || targetDeptId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department scope cannot be determined.");
        }
        enforceDepartmentScope(targetDeptId);

        final String finalDeptId = targetDeptId;
        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(finalDeptId)
                .orElseGet(() -> createDefaultProgress(finalDeptId, hodEmail));

        Set<String> existingCompleted = new LinkedHashSet<>();
        if (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()) {
            for (String s : progress.getCompletedSteps().split(",")) {
                String norm = normalizeHodStepName(s);
                if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
            }
        }

        // Add newly completed step(s)
        if (completedStepsList != null && !completedStepsList.isEmpty()) {
            for (Object item : completedStepsList) {
                String norm = normalizeHodStepName(item);
                if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
            }
        } else if (completedStep != null && !completedStep.isBlank()) {
            String norm = normalizeHodStepName(completedStep);
            if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
        } else if (targetStep != null) {
            String norm = normalizeHodStepName(targetStep);
            if (norm != null && !norm.isBlank()) existingCompleted.add(norm);
        }

        List<String> ALL_STEPS = List.of("master_courses", "batch", "coordinators", "outcomes", "review");
        List<String> completed = ALL_STEPS.stream().filter(existingCompleted::contains).toList();
        List<String> pending = ALL_STEPS.stream().filter(s -> !existingCompleted.contains(s)).toList();

        int currentStep;
        if (completed.size() == ALL_STEPS.size()) {
            currentStep = 1;
        } else if (targetStep != null && targetStep >= 1 && targetStep <= 5) {
            currentStep = (targetStep < 5 ? targetStep + 1 : 1);
        } else {
            currentStep = progress.getCurrentStep() != null ? progress.getCurrentStep() : 1;
        }

        SetupStepStatus overallStatus;
        if (completed.size() == ALL_STEPS.size()) {
            overallStatus = SetupStepStatus.COMPLETED;
        } else if (completed.isEmpty()) {
            overallStatus = SetupStepStatus.NOT_STARTED;
        } else {
            overallStatus = SetupStepStatus.IN_PROGRESS;
        }

        progress.setCurrentStep(currentStep);
        progress.setOverallStatus(overallStatus);
        progress.setCompletedSteps(String.join(",", completed));
        progress.setPendingSteps(String.join(",", pending));
        progress.setUpdatedAt(ZonedDateTime.now());

        if (hodEmail != null && !hodEmail.isBlank()) {
            progress.setHodEmail(hodEmail.trim());
        }

        hodSetupProgressRepository.save(progress);

        System.out.println("[AcademicService] HOD setup progress updated | targetDeptId=" + targetDeptId + " | currentStep=" + currentStep + " | completed=" + completed + " | pending=" + pending);

        return buildHodSetupProgressDto(progress);
    }

    private String normalizeHodStepName(Object step) {
        if (step == null) return null;
        String s = String.valueOf(step).trim().toLowerCase();
        return switch (s) {
            case "1", "master_course", "master_courses", "course", "courses", "mastercourse" -> "master_courses";
            case "2", "batch", "batches", "batch_setup" -> "batch";
            case "3", "coordinator", "coordinators", "coordinator_allocation", "allocation", "programme_coordinator", "programme_coordinators" -> "coordinators";
            case "4", "outcome", "outcomes", "po_pso", "po_pso_peo", "pos", "peo", "peos" -> "outcomes";
            case "5", "review", "confirm", "review_confirm", "review_and_confirm" -> "review";
            default -> s;
        };
    }

    private HodSetupProgress createDefaultProgress(
            String departmentId,
            String hodEmail) {

        return HodSetupProgress.builder()
                .id("progress-dept-" + departmentId)
                .departmentId(departmentId)
                .hodEmail(hodEmail)
                .currentStep(1)
                .overallStatus(SetupStepStatus.IN_PROGRESS)
                .completedSteps("")
                .pendingSteps("master_courses,batch,coordinators,outcomes,review")
                .build();
    }

    private void validateDepartmentId(String departmentId) {

        if (departmentId == null || departmentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Department ID is required"
            );
        }
    }

    private void validateStepNumber(Integer stepNumber) {

        if (stepNumber == null || stepNumber < 1 || stepNumber > 5) {
            throw new IllegalArgumentException(
                    "Step number must be between 1 and 5"
            );
        }
    }

    private HodSetupProgressDto buildHodSetupProgressDto(
            HodSetupProgress progress) {

        List<String> completedList =
                progress.getCompletedSteps() != null
                        && !progress.getCompletedSteps().isBlank()
                        ? Arrays.asList(
                        progress.getCompletedSteps().split(",")
                )
                        : Collections.emptyList();

        List<String> pendingList =
                progress.getPendingSteps() != null
                        && !progress.getPendingSteps().isBlank()
                        ? Arrays.asList(
                        progress.getPendingSteps().split(",")
                )
                        : Collections.emptyList();

        return HodSetupProgressDto.builder()
                .id(progress.getId())
                .departmentId(progress.getDepartmentId())
                .hodEmail(progress.getHodEmail())
                .currentStep(progress.getCurrentStep())
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completedList)
                .pendingSteps(pendingList)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional
    public HodSetupProgressDto completeHodSetup(
            String departmentId,
            String hodEmail) {
        System.out.println("[AcademicService] completeHodSetup called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        validateDepartmentId(targetDeptId);
        enforceDepartmentScope(targetDeptId);

        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(targetDeptId)
                .orElseGet(() -> createDefaultProgress(
                        targetDeptId,
                        hodEmail
                ));

        progress.setCurrentStep(1);

        progress.setCompletedSteps(
                "master_courses,batch,coordinators,outcomes,review"
        );

        progress.setPendingSteps("");

        progress.setOverallStatus(
                SetupStepStatus.COMPLETED
        );

        progress.setUpdatedAt(ZonedDateTime.now());

        if (hodEmail != null && !hodEmail.isBlank()) {
            progress.setHodEmail(hodEmail.trim());
        }

        hodSetupProgressRepository.save(progress);

        System.out.println("[AcademicService] HOD setup marked as COMPLETED for targetDeptId: " + targetDeptId);

        return buildHodSetupProgressDto(progress);
    }

    private String resolveTargetProgId(String programmeId, String coordinatorEmail) {
        CurrentUserScope scope = getScope();
        String effectiveEmail = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? coordinatorEmail.trim().toLowerCase()
                : (scope != null && scope.getEmail() != null ? scope.getEmail().trim().toLowerCase() : null);

        if (scope != null && scope.isProgrammeCoordinator()) {
            if (programmeId != null && !programmeId.isBlank()) {
                enforceProgrammeScope(programmeId.trim());
                return programmeId.trim();
            }
            if (scope.getProgrammeId() != null && !scope.getProgrammeId().isBlank()) {
                return scope.getProgrammeId().trim();
            }
            if (effectiveEmail != null && !effectiveEmail.isBlank()) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(effectiveEmail);
                if (batches != null && !batches.isEmpty()) {
                    return batches.get(0).getMasterProgrammeId();
                }
            }
            return scope.getProgrammeId();
        }

        if (programmeId != null && !programmeId.isBlank()) {
            return programmeId.trim();
        }
        if (effectiveEmail != null && !effectiveEmail.isBlank()) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCase(effectiveEmail);
            if (batches != null && !batches.isEmpty()) {
                return batches.get(0).getMasterProgrammeId();
            }
            List<MasterProgramme> list = masterProgrammeRepository.findByDeletedAtIsNull();
            MasterProgramme p = list.stream().filter(pr -> (pr.getCoordinatorEmail() != null && effectiveEmail.equalsIgnoreCase(pr.getCoordinatorEmail().trim())) || (pr.getCoordinator() != null && effectiveEmail.equalsIgnoreCase(pr.getCoordinator().trim()))).findFirst().orElse(null);
            if (p != null) return p.getId();
        }
        if (scope != null && scope.getProgrammeId() != null) {
            return scope.getProgrammeId();
        }
        return null;
    }

    // --- MasterProgramme Coordinator Summary & Setup Progress ---
    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSummaryDto getProgrammeCoordinatorSummary(String coordinatorEmail, String programmeId) {
        System.out.println("[AcademicService] getProgrammeCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail + " | programmeId: " + programmeId);

        String targetProgId = resolveTargetProgId(programmeId, coordinatorEmail);
        if (targetProgId == null || targetProgId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterProgramme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        MasterProgramme prog = masterProgrammeRepository.findByIdAndDeletedAtIsNull(targetProgId)
                .orElseThrow(() -> new ResourceNotFoundException("MasterProgramme not found: " + targetProgId));
        enrichProgrammeCoordinator(prog);

        List<MasterProgramme> assignedProgrammes = List.of(prog);
        CurrentUserScope scope = getScope();
        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            assignedProgrammes = masterProgrammeRepository.findByDeletedAtIsNull();
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        } else if (scope != null && scope.isHod()) {
            assignedProgrammes = getAllProgrammes();
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        } else if (scope != null && scope.isDirector()) {
            assignedProgrammes = getProgrammesBySchool(scope.getRequiredSchoolId());
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        }

        String resolvedName = "MasterProgramme Coordinator";
        String resolvedEmail = coordinatorEmail != null ? coordinatorEmail : "";

        if (prog.getCoordinator() != null && !prog.getCoordinator().isBlank()) {
            resolvedName = prog.getCoordinator();
        }
        if (resolvedEmail.isBlank() && prog.getCoordinatorEmail() != null && !prog.getCoordinatorEmail().isBlank()) {
            resolvedEmail = prog.getCoordinatorEmail();
        }

        List<MasterCourse> courses = masterCourseRepository.findByMasterProgrammeId(targetProgId);
        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(targetProgId);
        List<ProgrammeOutcome> pos = (!batches.isEmpty() ? programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batches.get(0).getId()) : List.of());
        List<ProgrammeSpecificOutcome> psos = (!batches.isEmpty() ? programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batches.get(0).getId()) : List.of());
        List<PeoOutcome> peos = (!batches.isEmpty() ? peoOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batches.get(0).getId()) : List.of());

        ProgrammeCoordinatorSetupProgressDto progressDto = getProgrammeCoordinatorSetupProgress(coordinatorEmail, targetProgId);

        return ProgrammeCoordinatorSummaryDto.builder()
                .programmeId(prog.getId())
                .programmeCode(prog.getCode())
                .programmeName(prog.getName())
                .departmentId(prog.getDepartmentId())
                .departmentName(prog.getDepartmentName())
                .coordinatorName(resolvedName)
                .coordinatorEmail(resolvedEmail)
                .durationYears(prog.getDurationYears())
                .courseCount(courses.size())
                .activePOsCount(pos.size())
                .activePSOsCount(psos.size())
                .activePEOsCount(peos.size())
                .activeBatchesCount(batches.size())
                .pendingVerificationsCount(0)
                .assignedProgrammes(assignedProgrammes)
                .setupProgress(progressDto)
                .build();
    }

    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSetupProgressDto getProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId, String batchId) {
        System.out.println("[AcademicService] getProgrammeCoordinatorSetupProgress called | coordinatorEmail: " + coordinatorEmail + " | programmeId: " + programmeId + " | batchId: " + batchId);

        String targetProgId = resolveTargetProgId(programmeId, coordinatorEmail);
        if (targetProgId == null || targetProgId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterProgramme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        String targetBatchId = batchId;
        if (targetBatchId == null || targetBatchId.isBlank()) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(targetProgId);
            targetBatchId = !batches.isEmpty() ? batches.get(0).getId() : null;
        }

        final String finalProgId = targetProgId;
        final String finalBatchId = targetBatchId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeBatchId(finalBatchId)
                .orElseGet(() -> createDefaultPcProgress(finalProgId, finalBatchId, coordinatorEmail));

        return buildPcSetupProgressDto(progress);
    }

    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSetupProgressDto getProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId) {
        return getProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, null);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto updateProgrammeCoordinatorSetupProgress(
            String coordinatorEmail, String programmeId, String batchId, Integer stepNumber, Map<String, Object> body) {
        System.out.println("[AcademicService] updateProgrammeCoordinatorSetupProgress called | programmeId: " + programmeId + " | batchId: " + batchId + " | stepNumber: " + stepNumber + " | coordinatorEmail: " + coordinatorEmail);

        String targetProgId = resolveTargetProgId(programmeId, coordinatorEmail);
        if (targetProgId == null || targetProgId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterProgramme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        String targetBatchId = batchId;
        if (targetBatchId == null || targetBatchId.isBlank()) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(targetProgId);
            targetBatchId = !batches.isEmpty() ? batches.get(0).getId() : "batch-" + targetProgId;
        }

        final String finalProgId = targetProgId;
        final String finalBatchId = targetBatchId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeBatchId(finalBatchId)
                .orElseGet(() -> createDefaultPcProgress(finalProgId, finalBatchId, coordinatorEmail));

        progress.setBatchId(finalBatchId);

        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            progress.setCoordinatorEmail(coordinatorEmail);
        }

        Set<String> completedSet = new LinkedHashSet<>();
        if (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()) {
            for (String s : progress.getCompletedSteps().split(",")) {
                String clean = s.trim();
                if (!clean.isEmpty()) {
                    completedSet.add(clean);
                    String norm = normalizePcStepName(clean);
                    if (norm != null && !norm.isBlank()) completedSet.add(norm);
                }
            }
        }

        if (body != null && body.containsKey("completedSteps")) {
            Object csObj = body.get("completedSteps");
            if (csObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        String s = item.toString().trim();
                        if (!s.isEmpty()) {
                            completedSet.add(s);
                            String norm = normalizePcStepName(s);
                            if (norm != null && !norm.isBlank()) completedSet.add(norm);
                        }
                    }
                }
            } else if (csObj instanceof String csStr) {
                for (String s : csStr.split(",")) {
                    String clean = s.trim();
                    if (!clean.isEmpty()) {
                        completedSet.add(clean);
                        String norm = normalizePcStepName(clean);
                        if (norm != null && !norm.isBlank()) completedSet.add(norm);
                    }
                }
            }
        } else if (body != null && body.containsKey("completedStep")) {
            String s = String.valueOf(body.get("completedStep")).trim();
            if (!s.isEmpty()) {
                completedSet.add(s);
                String norm = normalizePcStepName(s);
                if (norm != null && !norm.isBlank()) completedSet.add(norm);
            }
        } else if (stepNumber != null) {
            completedSet.add(String.valueOf(stepNumber));
            String norm = normalizePcStepName(stepNumber);
            if (norm != null && !norm.isBlank()) completedSet.add(norm);
        }

        List<String> ALL_PC_STEPS = List.of("courses", "po_pso_target", "indirect_attainment", "programme_atr", "review");
        List<String> canonicalCompleted = ALL_PC_STEPS.stream().filter(completedSet::contains).toList();

        int step = stepNumber != null ? stepNumber : (progress.getCurrentStep() != null ? progress.getCurrentStep() : 1);
        progress.setCurrentStep(step);

        progress.setCompletedSteps(String.join(",", completedSet));

        if (canonicalCompleted.size() == ALL_PC_STEPS.size() || completedSet.containsAll(List.of("0", "1", "2", "3")) || completedSet.contains("verify&finish") || completedSet.contains("review")) {
            progress.setPendingSteps("");
            progress.setOverallStatus(SetupStepStatus.COMPLETED);
        } else {
            Set<String> allSteps = new LinkedHashSet<>(ALL_PC_STEPS);
            allSteps.addAll(List.of("0", "1", "2", "3"));
            allSteps.removeAll(completedSet);
            progress.setPendingSteps(String.join(",", allSteps));
            if (!completedSet.isEmpty()) {
                progress.setOverallStatus(SetupStepStatus.IN_PROGRESS);
            } else {
                progress.setOverallStatus(SetupStepStatus.NOT_STARTED);
            }
        }

        pcSetupProgressRepository.save(progress);
        return buildPcSetupProgressDto(progress);
    }

    private String normalizePcStepName(Object step) {
        if (step == null) return null;
        String s = String.valueOf(step).trim().toLowerCase();
        return switch (s) {
            case "1", "0", "course", "courses", "add_courses", "add_course", "course_setup", "programme setup", "programme_setup" -> "courses";
            case "2", "target", "targets", "po_pso_target", "po_pso_targets", "po_target", "po_targets", "po/pso target", "po/pso targets" -> "po_pso_target";
            case "3", "indirect", "indirect_attainment", "survey", "exit_survey", "programme_survey", "indirect_programme_batch_attainment", "indirect attainment" -> "indirect_attainment";
            case "4", "atr", "programme_atr", "programme_batch_atr", "atrs", "programme atr" -> "programme_atr";
            case "5", "review", "confirm", "review_confirm", "review_and_confirm", "verify", "verify&finish", "verify_and_finish" -> "review";
            default -> s;
        };
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto updateProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId, Integer stepNumber) {
        return updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, null, stepNumber, null);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto completeProgrammeCoordinatorSetup(String coordinatorEmail, String programmeId, String batchId) {
        System.out.println("[AcademicService] completeProgrammeCoordinatorSetup called | programmeId: " + programmeId + " | batchId: " + batchId + " | coordinatorEmail: " + coordinatorEmail);
        Map<String, Object> body = Map.of("completedSteps", List.of("courses", "po_pso_target", "indirect_attainment", "programme_atr", "review", "0", "1", "2", "3"));
        return updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, batchId, 0, body);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto completeProgrammeCoordinatorSetup(String coordinatorEmail, String programmeId) {
        return completeProgrammeCoordinatorSetup(coordinatorEmail, programmeId, null);
    }

    private ProgrammeCoordinatorSetupProgress createDefaultPcProgress(String programmeId, String batchId, String coordinatorEmail) {
        String finalBatchId = batchId;
        if (finalBatchId == null || finalBatchId.isBlank()) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(programmeId);
            finalBatchId = !batches.isEmpty() ? batches.get(0).getId() : "batch-" + programmeId;
        }
        return ProgrammeCoordinatorSetupProgress.builder()
                .id("pcprog-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeBatchId(finalBatchId)
                .coordinatorEmail(coordinatorEmail)
                .currentStep(0)
                .overallStatus(SetupStepStatus.IN_PROGRESS)
                .completedSteps("")
                .pendingSteps("courses,po_pso_target,indirect_attainment,programme_atr,review,0,1,2,3")
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private ProgrammeCoordinatorSetupProgress createDefaultPcProgress(String programmeId, String coordinatorEmail) {
        return createDefaultPcProgress(programmeId, null, coordinatorEmail);
    }

    private ProgrammeCoordinatorSetupProgressDto buildPcSetupProgressDto(ProgrammeCoordinatorSetupProgress progress) {
        List<String> completed = progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()
                ? Arrays.stream(progress.getCompletedSteps().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        List<String> pending = progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank()
                ? Arrays.stream(progress.getPendingSteps().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        String progId = progress.getProgrammeId();
        if (progress.getProgrammeBatchId() != null) {
            ProgrammeBatch b = programmeBatchRepository.findById(progress.getProgrammeBatchId()).orElse(null);
            if (b != null && b.getMasterProgrammeId() != null) {
                progId = b.getMasterProgrammeId();
            }
        }

        return ProgrammeCoordinatorSetupProgressDto.builder()
                .id(progress.getId())
                .programmeId(progId)
                .batchId(progress.getBatchId())
                .coordinatorEmail(progress.getCoordinatorEmail())
                .currentStep(progress.getCurrentStep() != null ? progress.getCurrentStep() : 0)
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completed)
                .pendingSteps(pending)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHodCoordinators(String departmentId) {
        CurrentUserScope scope = getScope();
        String targetDeptId = departmentId;
        if (scope != null && scope.isHod()) {
            targetDeptId = scope.getRequiredDepartmentId();
        }
        if (targetDeptId != null && !targetDeptId.isBlank()) {
            enforceDepartmentScope(targetDeptId);
        }
        List<MasterProgramme> progs = (targetDeptId != null && !targetDeptId.isBlank())
                ? masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(targetDeptId)
                : getAllProgrammes();

        List<Map<String, Object>> list = new ArrayList<>();
        for (MasterProgramme p : progs) {
            enrichProgrammeCoordinator(p);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("programmeId", p.getId());
            item.put("code", p.getCode());
            item.put("programmeCode", p.getCode());
            item.put("name", p.getName());
            item.put("programmeName", p.getName());
            item.put("durationYears", p.getDurationYears() != null ? p.getDurationYears() : 4);
            item.put("departmentId", p.getDepartmentId());
            item.put("departmentName", p.getDepartmentName());
            item.put("coordinator", p.getCoordinator() != null ? p.getCoordinator() : "Not Assigned");
            item.put("coordinatorName", p.getCoordinator() != null ? p.getCoordinator() : "Not Assigned");
            item.put("coordinatorEmail", p.getCoordinatorEmail() != null ? p.getCoordinatorEmail() : "");
            item.put("status", p.getStatus() != null ? p.getStatus() : "ACTIVE");
            item.put("assignedDate", p.getUpdatedAt() != null ? p.getUpdatedAt().toLocalDate().toString() : "2025-06-15");
            list.add(item);
        }
        return list;
    }

    @Transactional
    public Map<String, Object> assignHodCoordinator(Map<String, Object> payload) {
        String progId = payload != null && payload.get("programmeId") != null
                ? payload.get("programmeId").toString()
                : (payload != null && payload.get("id") != null ? payload.get("id").toString() : null);
        String name = payload != null && payload.get("coordinatorName") != null
                ? payload.get("coordinatorName").toString()
                : (payload != null && payload.get("coordinator") != null ? payload.get("coordinator").toString() : "");
        String email = payload != null && payload.get("coordinatorEmail") != null
                ? payload.get("coordinatorEmail").toString()
                : "";

        if (progId != null) {
            enforceProgrammeScope(progId);
            MasterProgramme p = masterProgrammeRepository.findByIdAndDeletedAtIsNull(progId).orElse(null);
            if (p != null) {
                p.setCoordinator(name);
                p.setCoordinatorEmail(email);
                saveProgramme(p);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "MasterProgramme coordinator assigned successfully.");
        if (auditLogService != null && progId != null) {
            auditLogService.recordSuccess(com.dypiu.nba.audit.AuditAction.ASSIGN_COORDINATOR, com.dypiu.nba.audit.ResourceType.MASTER_PROGRAMME, progId, null, null, "Assigned PC coordinator " + name, java.util.Map.of("coordinatorEmail", email != null ? email : ""));
        }
        return res;
    }

    @Transactional
    public Map<String, Object> allocateCourses(String programmeId, String batchId, List<Map<String, Object>> allocations) {
        return allocateCourses(programmeId, batchId, allocations, true);
    }

    @Transactional
    public Map<String, Object> allocateCourses(String programmeId, String batchId, List<Map<String, Object>> allocations, boolean submit) {
        if (programmeId != null) {
            enforceProgrammeScope(programmeId);
            if (isAllocationApproved(programmeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify approved Course Allocation. A revision must be requested first.");
            }
        }
        if (allocations != null) {
            for (Map<String, Object> item : allocations) {
                String courseId = item.get("courseId") != null ? item.get("courseId").toString() : null;
                String email = item.get("coordinatorEmail") != null ? item.get("coordinatorEmail").toString() : "";
                String name = item.get("courseCoordinatorName") != null ? item.get("courseCoordinatorName").toString() : (item.get("coordinator") != null ? item.get("coordinator").toString() : "");

                if (courseId != null) {
                    enforceCourseScope(courseId);
                    MasterCourse course = masterCourseRepository.findById(courseId).orElse(null);
                    if (course != null) {
                        course.setCoordinator(name);
                        course.setFaculty(name);
                        course.setAssignedFaculty(name + " (" + email + ")");
                        masterCourseRepository.save(course);
                    }

                    if (batchId != null && !batchId.isBlank()) {
                        // Resolve the coordinator user
                        User coordinatorUser = null;
                        if (!email.isBlank()) {
                            coordinatorUser = userRepository.findByEmail(email).orElse(null);
                        }
                        
                        // Check if ProgrammeBatchCourse already exists for courseId + batchId
                        List<ProgrammeBatchCourse> existingOfferings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
                        ProgrammeBatchCourse targetOffering = existingOfferings.stream()
                                .filter(o -> batchId.equals(o.getProgrammeBatchId()))
                                .findFirst()
                                .orElse(null);
                                
                        String semStr = item.get("semester") != null ? item.get("semester").toString() : (course != null ? course.getSemester() : null);
                        Integer parsedSem = (semStr != null && semStr.trim().matches("\\d+")) ? Integer.parseInt(semStr.trim()) : 1;

                        if (targetOffering == null) {
                            // Create exactly one ProgrammeBatchCourse
                            targetOffering = ProgrammeBatchCourse.builder()
                                    .id("off-" + UUID.randomUUID().toString().substring(0, 8))
                                    .masterCourseId(courseId)
                                    .programmeBatchId(batchId)
                                    .semester(parsedSem)
                                    .courseCoordinatorName(name)
                                    .assignedFaculty(name + " (" + email + ")")
                                    .status("ACTIVE")
                                    .build();
                        } else {
                            if (item.get("semester") != null || course != null && course.getSemester() != null) {
                                targetOffering.setSemester(parsedSem);
                            }
                        }
                        
                        // Update coordinator info
                        targetOffering.setCourseCoordinatorName(name);
                        targetOffering.setAssignedFaculty(name + " (" + email + ")");
                        if (coordinatorUser != null) {
                            targetOffering.setCourseCoordinatorId(coordinatorUser.getId());
                        }
                        
                        programmeBatchCourseRepository.save(targetOffering);
                    } else {
                        // Fallback to update existing offerings if batchId is not provided
                        String semStr = item.get("semester") != null ? item.get("semester").toString() : (course != null ? course.getSemester() : null);
                        Integer parsedSem = (semStr != null && semStr.trim().matches("\\d+")) ? Integer.parseInt(semStr.trim()) : null;
                        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
                        for (ProgrammeBatchCourse off : offerings) {
                            off.setCourseCoordinatorName(name);
                            off.setAssignedFaculty(name + " (" + email + ")");
                            if (parsedSem != null) {
                                off.setSemester(parsedSem);
                            }
                            if (!email.isBlank()) {
                                userRepository.findByEmail(email).ifPresent(u -> off.setCourseCoordinatorId(u.getId()));
                            }
                            programmeBatchCourseRepository.save(off);
                        }
                    }
                }
            }
        }

        if (submit) {
            ApprovalRequest req = ApprovalRequest.builder()
                    .id("app-alloc-" + UUID.randomUUID().toString().substring(0, 8))
                    .type(ApprovalType.COURSE_ALLOCATION)
                    .title("MasterCourse Allocation for MasterProgramme " + programmeId)
                    .masterProgrammeId(programmeId)
                    .resourceId("allocation-" + programmeId)
                    .status(ApprovalStatus.PENDING)
                    .submittedBy("MasterProgramme Coordinator")
                    .submittedAt(ZonedDateTime.now())
                    .remarks("Allocations submitted for HOD review.")
                    .build();
            approvalRequestRepository.save(req);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", submit ? "MasterCourse allocations saved and submitted for verification." : "MasterCourse allocations saved successfully.");
        return res;
    }

    public boolean isAllocationApproved(String programmeId) {
        if (programmeId == null || programmeId.isBlank()) return false;
        String progId = programmeId.replace("allocation-", "").replace("allocation_", "").replace("allocation", "");
        return approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && (progId.equalsIgnoreCase(a.getMasterProgrammeId()) || progId.equalsIgnoreCase(a.getProgrammeId()) || programmeId.equalsIgnoreCase(a.getResourceId())))
                .max(java.util.Comparator.comparing(ApprovalRequest::getUpdatedAt, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .map(a -> a.getStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConsolidatedOutcomes(String programmeId, String batchId) {
        if (programmeId != null && !programmeId.isBlank()) {
            enforceProgrammeScope(programmeId.trim());
        }
        if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId.trim());
        }
        if (programmeId == null || programmeId.isBlank()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("programmeId", null);
            data.put("batchId", batchId);
            data.put("pos", Collections.emptyList());
            data.put("psos", Collections.emptyList());
            data.put("peos", Collections.emptyList());
            return data;
        }
        String pId = programmeId.trim();
        String targetBatchId = (batchId != null && !batchId.isBlank()) ? batchId.trim() : null;
        if (targetBatchId == null) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(pId);
            for (ProgrammeBatch b : batches) {
                if (!programmeOutcomeRepository.findByProgrammeBatchId(b.getId()).isEmpty()) {
                    targetBatchId = b.getId();
                    break;
                }
            }
            if (targetBatchId == null && !batches.isEmpty()) {
                targetBatchId = batches.get(0).getId();
            }
        }
        List<ProgrammeOutcome> pos = (targetBatchId != null) ? new ArrayList<>(programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId)) : new ArrayList<>();
        for (ProgrammeOutcome po : pos) {
            List<PoCompetency> comps = new ArrayList<>(poCompetencyRepository.findByPoIdOrderByCodeAsc(po.getId()));
            comps.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            po.setCompetencies(comps);
        }
        pos.sort(Comparator.comparing(ProgrammeOutcome::getCode, NATURAL_CODE_COMPARATOR));

        List<ProgrammeSpecificOutcome> psos = (targetBatchId != null) ? new ArrayList<>(programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId)) : new ArrayList<>();
        for (ProgrammeSpecificOutcome pso : psos) {
            List<PsoCompetency> comps = new ArrayList<>(psoCompetencyRepository.findByPsoIdOrderByCodeAsc(pso.getId()));
            comps.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
            pso.setCompetencies(comps);
        }
        psos.sort(Comparator.comparing(ProgrammeSpecificOutcome::getCode, NATURAL_CODE_COMPARATOR));

        List<PeoOutcome> peos = (targetBatchId != null) ? new ArrayList<>(peoOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId)) : new ArrayList<>();
        peos.sort(Comparator.comparing(PeoOutcome::getCode, NATURAL_CODE_COMPARATOR));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("programmeId", pId);
        data.put("batchId", batchId != null ? batchId : targetBatchId);
        data.put("pos", pos);
        data.put("psos", psos);
        data.put("peos", peos);
        return data;
    }

    @Transactional
    public Map<String, Object> saveConsolidatedOutcomes(Map<String, Object> payload) {
        String progId = payload != null && payload.get("programmeId") != null ? payload.get("programmeId").toString().trim() : null;
        if (progId == null || progId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MasterProgramme ID is required to save outcomes.");
        }
        enforceProgrammeScope(progId);

        String batchId = payload != null && payload.get("batchId") != null ? payload.get("batchId").toString().trim() : null;
        String targetBatchId = (batchId != null && !batchId.isBlank()) ? batchId : null;
        if (targetBatchId == null) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(progId);
            if (!batches.isEmpty()) {
                targetBatchId = batches.get(0).getId();
            } else {
                targetBatchId = progId;
            }
        }

        // 1. Process and save POs
        if (payload != null && payload.get("pos") instanceof List<?> poList) {
            List<ProgrammeOutcome> existingPOs = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId);
            if (!existingPOs.isEmpty()) {
                for (ProgrammeOutcome existingPo : existingPOs) {
                    poCompetencyRepository.deleteByPoId(existingPo.getId());
                }
                poCompetencyRepository.flush();
                programmeOutcomeRepository.deleteAll(existingPOs);
                programmeOutcomeRepository.flush();
            }

            for (Object obj : poList) {
                if (obj instanceof Map<?, ?> poMap) {
                    String code = poMap.get("code") != null ? poMap.get("code").toString() : null;
                    String statement = poMap.get("statement") != null ? poMap.get("statement").toString() : "";
                    BigDecimal target = new BigDecimal("2.50");
                    if (poMap.get("target") != null) {
                        try {
                            target = new BigDecimal(poMap.get("target").toString());
                        } catch (Exception ignored) {}
                    }
                    if (code != null && !code.isBlank()) {
                        String poId = poMap.get("id") != null ? poMap.get("id").toString() : null;
                        if (poId == null || poId.isBlank()) {
                            poId = "po-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6);
                        }
                        ProgrammeOutcome po = ProgrammeOutcome.builder()
                                .id(poId)
                                .programmeBatchId(targetBatchId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .target(target)
                                .build();
                        programmeOutcomeRepository.save(po);

                        if (poMap.get("competencies") instanceof List<?> compList) {
                            List<PoCompetency> compsToSave = new ArrayList<>();
                            int cIdx = 1;
                            for (Object cObj : compList) {
                                if (cObj instanceof Map<?, ?> compMap) {
                                    String cStatement = compMap.get("statement") != null ? compMap.get("statement").toString() : "";
                                    if (cStatement.isBlank()) continue;
                                    String cCode = compMap.get("code") != null ? compMap.get("code").toString() : (po.getCode() + "." + cIdx);
                                    String cId = compMap.get("id") != null ? compMap.get("id").toString() : null;
                                    if (cId == null || cId.isBlank() || cId.startsWith("comp-")) {
                                        cId = "pocomp-" + UUID.randomUUID().toString().substring(0, 8);
                                    }
                                    cIdx++;
                                    compsToSave.add(PoCompetency.builder()
                                            .id(cId)
                                            .poId(po.getId())
                                            .code(cCode.trim())
                                            .statement(cStatement.trim())
                                            .build());
                                }
                            }
                            if (!compsToSave.isEmpty()) {
                                compsToSave.sort(Comparator.comparing(PoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                                poCompetencyRepository.saveAll(compsToSave);
                                poCompetencyRepository.flush();
                            }
                        }
                    }
                }
            }
        }

        // 2. Process and save PSOs
        if (payload != null && payload.get("psos") instanceof List<?> psoList) {
            List<ProgrammeSpecificOutcome> existingPSOs = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId);
            if (!existingPSOs.isEmpty()) {
                for (ProgrammeSpecificOutcome existingPso : existingPSOs) {
                    psoCompetencyRepository.deleteByPsoId(existingPso.getId());
                }
                psoCompetencyRepository.flush();
                programmeSpecificOutcomeRepository.deleteAll(existingPSOs);
                programmeSpecificOutcomeRepository.flush();
            }

            for (Object obj : psoList) {
                if (obj instanceof Map<?, ?> psoMap) {
                    String code = psoMap.get("code") != null ? psoMap.get("code").toString() : null;
                    String statement = psoMap.get("statement") != null ? psoMap.get("statement").toString() : "";
                    BigDecimal target = new BigDecimal("2.50");
                    if (psoMap.get("target") != null) {
                        try {
                            target = new BigDecimal(psoMap.get("target").toString());
                        } catch (Exception ignored) {}
                    }
                    if (code != null && !code.isBlank()) {
                        String psoId = psoMap.get("id") != null ? psoMap.get("id").toString() : null;
                        if (psoId == null || psoId.isBlank()) {
                            psoId = "pso-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6);
                        }
                        ProgrammeSpecificOutcome pso = ProgrammeSpecificOutcome.builder()
                                .id(psoId)
                                .programmeBatchId(targetBatchId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .target(target)
                                .build();
                        programmeSpecificOutcomeRepository.save(pso);

                        if (psoMap.get("competencies") instanceof List<?> compList) {
                            List<PsoCompetency> compsToSave = new ArrayList<>();
                            int cIdx = 1;
                            for (Object cObj : compList) {
                                if (cObj instanceof Map<?, ?> compMap) {
                                    String cStatement = compMap.get("statement") != null ? compMap.get("statement").toString() : "";
                                    if (cStatement.isBlank()) continue;
                                    String cCode = compMap.get("code") != null ? compMap.get("code").toString() : (pso.getCode() + "." + cIdx);
                                    String cId = compMap.get("id") != null ? compMap.get("id").toString() : null;
                                    if (cId == null || cId.isBlank() || cId.startsWith("comp-")) {
                                        cId = "psocomp-" + UUID.randomUUID().toString().substring(0, 8);
                                    }
                                    cIdx++;
                                    compsToSave.add(PsoCompetency.builder()
                                            .id(cId)
                                            .psoId(pso.getId())
                                            .code(cCode.trim())
                                            .statement(cStatement.trim())
                                            .build());
                                }
                            }
                            if (!compsToSave.isEmpty()) {
                                compsToSave.sort(Comparator.comparing(PsoCompetency::getCode, NATURAL_CODE_COMPARATOR));
                                psoCompetencyRepository.saveAll(compsToSave);
                                psoCompetencyRepository.flush();
                            }
                        }
                    }
                }
            }
        }

        // 3. Process and save PEOs
        if (payload != null && payload.get("peos") instanceof List<?> peoList) {
            List<PeoOutcome> existingPEOs = peoOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(targetBatchId);
            if (!existingPEOs.isEmpty()) {
                peoOutcomeRepository.deleteAll(existingPEOs);
                peoOutcomeRepository.flush();
            }

            for (Object obj : peoList) {
                if (obj instanceof Map<?, ?> peoMap) {
                    String code = peoMap.get("code") != null ? peoMap.get("code").toString() : null;
                    String statement = peoMap.get("statement") != null ? peoMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        String peoId = peoMap.get("id") != null ? peoMap.get("id").toString() : null;
                        if (peoId == null || peoId.isBlank()) {
                            peoId = "peo-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6);
                        }
                        PeoOutcome peo = PeoOutcome.builder()
                                .id(peoId)
                                .programmeBatchId(targetBatchId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        peoOutcomeRepository.save(peo);
                    }
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Outcomes saved successfully.");
        res.put("data", getConsolidatedOutcomes(progId, targetBatchId));
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseCoTargets(String courseId, String batchId) {
        if (courseId != null && !courseId.isBlank()) {
            enforceCourseScope(courseId);
        }
        if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId);
        }
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offeringId);

        Map<String, BigDecimal> targets = new LinkedHashMap<>();
        for (CourseOutcome co : cos) {
            targets.put(co.getCode(), co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50"));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", courseId);
        res.put("batchId", batchId);
        res.put("coTargets", targets);
        return res;
    }

    @Transactional
    public Map<String, Object> saveCourseCoTargets(String courseId, Map<String, Object> coTargets) {
        if (courseId != null && !courseId.isBlank()) {
            enforceCourseScope(courseId);
        }
        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offeringId);

        if (coTargets != null) {
            for (CourseOutcome co : cos) {
                if (coTargets.containsKey(co.getCode())) {
                    Object val = coTargets.get(co.getCode());
                    if (val instanceof Number) {
                        co.setTargetLevel(BigDecimal.valueOf(((Number) val).doubleValue()));
                    } else if (val instanceof String) {
                        try {
                            co.setTargetLevel(new BigDecimal((String) val));
                        } catch (Exception ignored) {}
                    }
                    courseOutcomeRepository.save(co);
                }
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "CO targets saved successfully.");
        res.put("data", getCourseCoTargets(courseId, null));
        return res;
    }
}
