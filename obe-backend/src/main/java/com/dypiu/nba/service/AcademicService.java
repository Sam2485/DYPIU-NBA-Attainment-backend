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
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final DirectorSetupProgressRepository directorSetupProgressRepository;
    private final HodSetupProgressRepository hodSetupProgressRepository;
    private final ProgrammeCoordinatorSetupProgressRepository pcSetupProgressRepository;
    private final CourseCoordinatorSetupProgressRepository ccSetupProgressRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final PeoOutcomeRepository peoOutcomeRepository;
    private final UserRepository userRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

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
        if (scope.isHod() || scope.isProgrammeCoordinator()) {
            String requiredDeptId = scope.getRequiredDepartmentId();
            if (departmentId != null && !departmentId.equals(requiredDeptId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned department scope.");
            }
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
            String requiredProgId = scope.getRequiredProgrammeId();
            if (!programmeId.equals(requiredProgId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned programme scope.");
            }
        }

        Programme prog = programmeRepository.findById(programmeId)
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
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        if (scope.isFaculty()) {
            List<CourseOffering> offerings = courseOfferingRepository.findByBatchId(batchId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                        ;
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to any Course Offering in this Batch.");
            }
            return;
        }

        enforceProgrammeScope(batch.getProgrammeId());
    }

    private void enforceCourseScope(String courseId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (courseId == null || courseId.isBlank()) return;
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        if (scope.isFaculty()) {
            List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
            boolean hasAssigned = offerings.stream().anyMatch(o -> {
                boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                        ;
                return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
            });
            if (!hasAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course.");
            }
            return;
        }

        enforceProgrammeScope(course.getProgrammeId());
    }

    private void enforceCourseOfferingScope(String offeringId) {
        CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;
        if (offeringId == null || offeringId.isBlank()) return;

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));

        if (scope.isFaculty()) {
            boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()))
                    ;
            boolean isFacultyAssigned = isCoordinator || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
            if (!isFacultyAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
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
        CourseOffering offering = courseOfferingRepository.findById(offeringOrCourseId).orElse(null);
        if (offering == null) {
            List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(offeringOrCourseId);
            offering = offerings.stream().findFirst().orElse(null);
        }
        if (offering == null) {
            throw new ResourceNotFoundException("Course offering not found: " + offeringOrCourseId);
        }
        boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()))
                ;
        if (!isCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned Course Coordinator can perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public com.dypiu.nba.dto.BatchContextDto getBatchContext(String batchId) {
        System.out.println("[AcademicService] getBatchContext called | batchId: " + batchId);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        enforceProgrammeScope(batch.getProgrammeId());

        Programme prog = programmeRepository.findById(batch.getProgrammeId()).orElse(null);
        Department dept = (prog != null && prog.getDepartmentId() != null) ? departmentRepository.findById(prog.getDepartmentId()).orElse(null) : null;
        School school = (dept != null && dept.getSchoolId() != null) ? schoolRepository.findById(dept.getSchoolId()).orElse(null) : null;

        List<Student> students = studentRepository.findByBatchId(batchId);
        List<CourseOffering> offerings = courseOfferingRepository.findByBatchId(batchId);
        Set<String> uniqueCourseIds = offerings.stream().map(CourseOffering::getCourseId).collect(Collectors.toSet());
        List<String> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());

        long completedAtrs = offeringIds.isEmpty() ? 0 : courseAtrRepository.findByCourseOfferingIdIn(offeringIds).stream()
                                                         .filter(a -> a.getStatus() == CourseAtrStatus.VERIFIED)
                                                         .count();

        String progAtrStatus = "DRAFT";
        if (prog != null) {
            Optional<ProgrammeAtr> patr = programmeAtrRepository.findByProgrammeIdAndBatchId(prog.getId(), batchId);
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

    @Transactional(readOnly = true)
    public List<CourseOffering> getCourseOfferingsByBatch(String batchId) {
        System.out.println("[AcademicService] getCourseOfferingsByBatch called | batchId: " + batchId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isFaculty()) {
            List<CourseOffering> offerings = (batchId != null && !batchId.isBlank())
                    ? courseOfferingRepository.findByBatchId(batchId)
                    : courseOfferingRepository.findAll();
            return offerings.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .collect(Collectors.toList());
        }
        if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId);
            return courseOfferingRepository.findByBatchId(batchId);
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            List<Batch> batches = batchRepository.findByProgrammeId(scope.getRequiredProgrammeId());
            Set<String> bIds = batches.stream().map(Batch::getId).collect(Collectors.toSet());
            return bIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(bIds);
        }
        if (scope != null && scope.isHod()) {
            List<Programme> progs = programmeRepository.findByDepartmentId(scope.getRequiredDepartmentId());
            List<String> pIds = progs.stream().map(Programme::getId).toList();
            List<Batch> batches = pIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(pIds);
            Set<String> bIds = batches.stream().map(Batch::getId).collect(Collectors.toSet());
            return bIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(bIds);
        }
        if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> dIds = depts.stream().map(Department::getId).toList();
            List<Programme> progs = dIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(dIds);
            List<String> pIds = progs.stream().map(Programme::getId).toList();
            List<Batch> batches = pIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(pIds);
            Set<String> bIds = batches.stream().map(Batch::getId).collect(Collectors.toSet());
            return bIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(bIds);
        }
        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            return courseOfferingRepository.findAll();
        }
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public CourseOffering getCourseOfferingById(String offeringId) {
        System.out.println("[AcademicService] getCourseOfferingById called | offeringId: " + offeringId);
        if (offeringId == null || offeringId.isBlank()) return null;
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + offeringId));
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        return offering;
    }

    @Transactional
    public CourseOffering saveCourseOffering(CourseOffering offering) {
        System.out.println("[AcademicService] saveCourseOffering called | courseId: " + (offering != null ? offering.getCourseId() : "null"));
        if (offering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course offering details cannot be null.");
        }
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        if (offering.getId() != null) {
            CourseOffering existing = courseOfferingRepository.findById(offering.getId()).orElse(null);
            if (existing != null) {
                if (existing.getBatchId() != null) enforceBatchScope(existing.getBatchId());
                if (existing.getCourseId() != null) enforceCourseScope(existing.getCourseId());
            }
        }
        if (offering.getId() == null) offering.setId("offering-" + UUID.randomUUID().toString().substring(0, 8));
        return courseOfferingRepository.save(offering);
    }

    @Transactional
    public void deleteCourseOffering(String id) {
        System.out.println("[AcademicService] deleteCourseOffering called | id: " + id);
        CourseOffering offering = courseOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + id));
        if (offering.getBatchId() != null) enforceBatchScope(offering.getBatchId());
        if (offering.getCourseId() != null) enforceCourseScope(offering.getCourseId());
        courseOfferingRepository.deleteById(id);
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
        List<Programme> schoolProgrammes = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);

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
            int progsCount = programmeRepository.findByDepartmentId(dept.getId()).size();
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

        School saved = schoolRepository.save(school);

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
        Department saved = departmentRepository.save(department);
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

        // Apply organizational scope isolation for Director, HOD, and Programme Coordinator
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
            String schoolId = scope.getRequiredSchoolId();
            String deptId = scope.getRequiredDepartmentId();
            users = users.stream()
                    .filter(u -> (u.getSchoolId() == null || u.getSchoolId().equals(schoolId))
                            && (u.getDepartmentId() == null || u.getDepartmentId().equals(deptId)))
                    .collect(Collectors.toList());
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
                            .programmeId(u.getProgrammeId())
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
    private void enrichProgrammeCoordinator(Programme programme) {
        if (programme == null) return;
        String coord = programme.getCoordinator();
        String coordEmail = programme.getCoordinatorEmail();

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
        System.out.println("[AcademicService] getCourseCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail);
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

        List<CourseOffering> allOfferings = courseOfferingRepository.findAll();
        final String searchEmail = email.toLowerCase();
        final String searchName = name.toLowerCase();
        final Long searchUserId = userId;

        List<CourseOffering> assignedOfferings = allOfferings.stream()
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

        Set<String> assignedCourseIds = assignedOfferings.stream().map(CourseOffering::getCourseId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Course> finalCourses = assignedCourseIds.isEmpty() ? Collections.emptyList() : courseRepository.findAllById(assignedCourseIds);

        for (Course course : finalCourses) {
            CourseOffering offering = assignedOfferings.stream()
                    .filter(o -> course.getId().equals(o.getCourseId()))
                    .findFirst()
                    .orElse(null);
            if (offering != null) {
                course.setCourseOfferingId(offering.getId());
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

        Course primaryCourse = !finalCourses.isEmpty() ? finalCourses.get(0) : null;

        CourseCoordinatorSetupProgressDto setupProgress = null;
        int coCount = 0;
        int poCount = 0;
        int psoCount = 0;

        if (primaryCourse != null) {
            setupProgress = getCourseCoordinatorSetupProgress(email, primaryCourse.getId());

            List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(primaryCourse.getId());
            String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : primaryCourse.getId();
            coCount = courseOutcomeRepository.findByCourseOfferingId(offeringId).size();

            String programmeId = primaryCourse.getProgrammeId();
            if (programmeId != null && !programmeId.isBlank()) {
                poCount = programmeOutcomeRepository.findByProgrammeId(programmeId).size();
                psoCount = programmeSpecificOutcomeRepository.findByProgrammeId(programmeId).size();
            }
        }

        if (poCount == 0) poCount = 12;
        if (psoCount == 0) psoCount = 2;

        return CourseCoordinatorSummaryDto.builder()
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
        if (courseId != null && !courseId.isBlank() && courseRepository.existsById(courseId)) {
            return courseId;
        }
        return courseId;
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSetupProgressDto getCourseCoordinatorSetupProgress(String coordinatorEmail, String courseId) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] getCourseCoordinatorSetupProgress called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId);
        if (targetCourseId != null && !targetCourseId.isBlank()) {
            if (courseOfferingRepository.existsById(targetCourseId)) {
                enforceCourseOfferingScope(targetCourseId);
            } else if (courseRepository.existsById(targetCourseId)) {
                enforceCourseScope(targetCourseId);
            }
        }
        CourseCoordinatorSetupProgress progress = (targetCourseId != null)
                ? ccSetupProgressRepository.findByCourseOfferingId(targetCourseId).orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + targetCourseId)
                        .courseOfferingId(targetCourseId)
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
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] updateCourseCoordinatorSetupProgress called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId + " | stepNumber: " + currentStep);
        if (targetCourseId != null && !targetCourseId.isBlank()) {
            if (courseOfferingRepository.existsById(targetCourseId)) {
                enforceCourseOfferingScope(targetCourseId);
            } else if (courseRepository.existsById(targetCourseId)) {
                enforceCourseScope(targetCourseId);
            }
        }
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByCourseOfferingId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .currentStep(1)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("cos,co_mapping,direct,indirect,attainment,course_atr")
                        .build());

        progress.setCurrentStep(currentStep != null ? currentStep : 1);
        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            progress.setCoordinatorEmail(coordinatorEmail);
        }
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(coordinatorEmail, targetCourseId);
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto completeCourseCoordinatorSetup(String coordinatorEmail, String courseId) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] completeCourseCoordinatorSetup called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId);
        if (targetCourseId != null && !targetCourseId.isBlank()) {
            if (courseOfferingRepository.existsById(targetCourseId)) {
                enforceCourseOfferingScope(targetCourseId);
                enforceCourseCoordinatorScope(targetCourseId);
            } else if (courseRepository.existsById(targetCourseId)) {
                enforceCourseScope(targetCourseId);
                enforceCourseCoordinatorScope(targetCourseId);
            }
        }
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByCourseOfferingId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .build());

        progress.setOverallStatus(SetupStepStatus.COMPLETED);
        progress.setCompletedSteps("cos,co_mapping,direct,indirect,attainment,course_atr");
        progress.setPendingSteps("");
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(coordinatorEmail, targetCourseId);
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        System.out.println("[AcademicService] getAllProgrammes called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            return getProgrammesBySchool(scope.getRequiredSchoolId());
        }
        if (scope != null && scope.isHod()) {
            return getProgrammesByDepartment(scope.getRequiredDepartmentId());
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            String progId = scope.getRequiredProgrammeId();
            Programme p = programmeRepository.findById(progId).orElse(null);
            if (p != null) {
                enforceProgrammeScope(p.getId());
                enrichProgrammeCoordinator(p);
                return List.of(p);
            }
            return Collections.emptyList();
        }
        List<Programme> list = programmeRepository.findAll();
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched all programmes (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public Programme getProgrammeById(String id) {
        System.out.println("[AcademicService] getProgrammeById called | id: " + id);
        if (id == null || id.isBlank()) return null;
        Programme p = programmeRepository.findById(id).orElse(null);
        if (p == null) return null;
        enforceProgrammeScope(p.getId());
        enrichProgrammeCoordinator(p);
        return p;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesByCoordinatorEmail(String coordinatorEmail) {
        System.out.println("[AcademicService] getProgrammesByCoordinatorEmail called | coordinatorEmail: " + coordinatorEmail);
        List<Programme> all = getAllProgrammes();
        if (coordinatorEmail == null || coordinatorEmail.isBlank()) {
            return all;
        }
        String emailTrim = coordinatorEmail.trim().toLowerCase();
        List<Programme> filtered = all.stream()
                .filter(p -> (p.getCoordinatorEmail() != null && emailTrim.equalsIgnoreCase(p.getCoordinatorEmail().trim()))
                        || (p.getCoordinator() != null && emailTrim.equalsIgnoreCase(p.getCoordinator().trim())))
                .toList();
        if (filtered.isEmpty()) {
            System.out.println("[AcademicService] No exact match for coordinatorEmail: " + coordinatorEmail + ", returning scoped " + all.size() + " programmes.");
            return all;
        }
        System.out.println("[AcademicService] Found " + filtered.size() + " programmes for coordinatorEmail: " + coordinatorEmail);
        return filtered;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesBySchool(String schoolId) {
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
            return getProgrammesByDepartment(scope.getRequiredDepartmentId());
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
        List<Programme> list = programmeRepository.findByDepartmentIdIn(deptIds);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesByDepartment(String departmentId) {
        System.out.println("[AcademicService] getProgrammesByDepartment called | departmentId: " + departmentId);
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isHod()) {
            String hodDeptId = scope.getRequiredDepartmentId();
            if (departmentId != null && !departmentId.isBlank() && !departmentId.equals(hodDeptId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot view programmes of a different department.");
            }
            departmentId = hodDeptId;
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            enforceDepartmentScope(departmentId);
            return getAllProgrammes();
        }
        if (departmentId == null || departmentId.isBlank()) {
            return getAllProgrammes();
        }
        enforceDepartmentScope(departmentId);
        List<Programme> list = programmeRepository.findByDepartmentId(departmentId);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for departmentId: " + departmentId);
        return list;
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        System.out.println("[AcademicService] saveProgramme called | id: " + (programme != null ? programme.getId() : "null") + " | name: " + (programme != null ? programme.getName() : "null") + " | coordinator: " + (programme != null ? programme.getCoordinator() : "null") + " | coordinatorEmail: " + (programme != null ? programme.getCoordinatorEmail() : "null"));
        if (programme == null) return null;

        if (programme.getId() != null) {
            Programme existing = programmeRepository.findById(programme.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getId());
            }
        }
        if (programme.getDepartmentId() != null) {
            enforceDepartmentScope(programme.getDepartmentId());
        }

        Programme targetProg = programme;
        if (programme.getId() != null) {
            Optional<Programme> existingOpt = programmeRepository.findById(programme.getId());
            if (existingOpt.isPresent()) {
                Programme existing = existingOpt.get();
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

        if (programme.getCoordinator() != null && !programme.getCoordinator().isBlank()) {
            targetProg.setCoordinator(programme.getCoordinator());
        }
        if (programme.getCoordinatorEmail() != null && !programme.getCoordinatorEmail().isBlank()) {
            targetProg.setCoordinatorEmail(programme.getCoordinatorEmail());
        }

        final Programme finalProg = targetProg;
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

        Programme saved = programmeRepository.save(finalProg);
        System.out.println("[AcademicService] Saved programme with id: " + saved.getId() + ", coordinator: " + saved.getCoordinator() + ", coordinatorEmail: " + saved.getCoordinatorEmail());
        return saved;
    }

    @Transactional
    public void deleteProgramme(String id) {
        System.out.println("[AcademicService] deleteProgramme called | id: " + id);
        enforceProgrammeScope(id);
        programmeRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted programme with id: " + id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        System.out.println("[AcademicService] getAllBatches called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> deptIds = depts.stream().map(Department::getId).toList();
            List<Programme> progs = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);
            List<String> progIds = progs.stream().map(Programme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isHod()) {
            List<Programme> progs = programmeRepository.findByDepartmentId(scope.getRequiredDepartmentId());
            List<String> progIds = progs.stream().map(Programme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            return batchRepository.findByProgrammeId(scope.getRequiredProgrammeId());
        }
        if (scope != null && scope.isFaculty()) {
            List<CourseOffering> allOfferings = courseOfferingRepository.findAll();
            Set<String> assignedBatchIds = allOfferings.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .map(CourseOffering::getBatchId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return assignedBatchIds.isEmpty() ? Collections.emptyList() : batchRepository.findAllById(assignedBatchIds);
        }
        List<Batch> list = batchRepository.findAll();
        System.out.println("[AcademicService] Fetched all batches (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesByProgramme(String programmeId) {
        System.out.println("[AcademicService] getBatchesByProgramme called | programmeId: " + programmeId);
        enforceProgrammeScope(programmeId);
        List<Batch> list = batchRepository.findByProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched batches (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional(readOnly = true)
    public Batch getBatchById(String id) {
        System.out.println("[AcademicService] getBatchById called | id: " + id);
        enforceBatchScope(id);
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
        return batch;
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesScoped(String programmeId, String userEmail, String role) {
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
            return batchRepository.findAll();
        }

        // 6. Fallback for other callers (Course Coordinator/Faculty)
        return getAllBatches();
    }

    @Transactional
    public Batch saveBatch(Batch batch) {
        System.out.println("[AcademicService] saveBatch called | name: " + (batch != null ? batch.getName() : "null"));
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch details cannot be null.");
        }
        if (batch.getId() != null) {
            Batch existing = batchRepository.findById(batch.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getProgrammeId());
            }
        }
        if (batch.getProgrammeId() != null) {
            enforceProgrammeScope(batch.getProgrammeId());
        }
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        Batch saved = batchRepository.save(batch);
        System.out.println("[AcademicService] Saved batch with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteBatch(String id) {
        System.out.println("[AcademicService] deleteBatch called | id: " + id);
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
        enforceProgrammeScope(batch.getProgrammeId());
        batchRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted batch with id: " + id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        System.out.println("[AcademicService] getAllCourses called");
        CurrentUserScope scope = getScope();
        if (scope != null && scope.isDirector()) {
            List<Department> depts = departmentRepository.findBySchoolId(scope.getRequiredSchoolId());
            List<String> deptIds = depts.stream().map(Department::getId).toList();
            List<Programme> progs = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);
            List<String> progIds = progs.stream().map(Programme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : courseRepository.findByProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isHod()) {
            List<Programme> progs = programmeRepository.findByDepartmentId(scope.getRequiredDepartmentId());
            List<String> progIds = progs.stream().map(Programme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : courseRepository.findByProgrammeIdIn(progIds);
        }
        if (scope != null && scope.isProgrammeCoordinator()) {
            return courseRepository.findByProgrammeId(scope.getRequiredProgrammeId());
        }
        if (scope != null && scope.isFaculty()) {
            List<CourseOffering> allOfferings = courseOfferingRepository.findAll();
            Set<String> assignedCourseIds = allOfferings.stream()
                    .filter(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    })
                    .map(CourseOffering::getCourseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return assignedCourseIds.isEmpty() ? Collections.emptyList() : courseRepository.findAllById(assignedCourseIds);
        }
        List<Course> list = courseRepository.findAll();
        System.out.println("[AcademicService] Fetched all courses (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public Course getCourseById(String id) {
        System.out.println("[AcademicService] getCourseById called | id: " + id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        enforceCourseScope(id);
        return course;
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByProgramme(String programmeId, String batchId) {
        System.out.println("[AcademicService] getCoursesByProgramme called | programmeId: " + programmeId + " | batchId: " + batchId);
        enforceProgrammeScope(programmeId);
        List<Course> list = courseRepository.findByProgrammeId(programmeId);
        
        if (batchId != null && !batchId.isBlank()) {
            for (Course course : list) {
                List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(course.getId());
                CourseOffering targetOffering = offerings.stream()
                        .filter(o -> batchId.equals(o.getBatchId()))
                        .findFirst()
                        .orElse(null);
                
                if (targetOffering != null) {
                    course.setSemester(targetOffering.getSemester() != null ? String.valueOf(targetOffering.getSemester()) : null);
                    course.setCoordinator(targetOffering.getCourseCoordinatorName());
                    course.setFaculty(targetOffering.getCourseCoordinatorName());
                    course.setAssignedFaculty(targetOffering.getAssignedFaculty());
                    course.setCourseOfferingId(targetOffering.getId());
                    
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
    public Course saveCourse(Course course) {
        System.out.println("[AcademicService] saveCourse called | name: " + (course != null ? course.getName() : "null"));
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course details cannot be null.");
        }
        if (course.getId() != null) {
            Course existing = courseRepository.findById(course.getId()).orElse(null);
            if (existing != null) {
                enforceProgrammeScope(existing.getProgrammeId());
            }
        }
        if (course.getProgrammeId() != null) {
            enforceProgrammeScope(course.getProgrammeId());
        }
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        Course saved = courseRepository.save(course);
        System.out.println("[AcademicService] Saved course with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteCourse(String id) {
        System.out.println("[AcademicService] deleteCourse called | id: " + id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        enforceProgrammeScope(course.getProgrammeId());
        courseRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted course with id: " + id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        System.out.println("[AcademicService] getStudentsByBatch called | batchId: " + batchId);
        if (batchId != null && !batchId.isBlank()) {
            enforceBatchScope(batchId);
        }
        List<Student> list = studentRepository.findByBatchId(batchId);
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
        List<Programme> programmes = programmeRepository.findByDepartmentId(deptId);
        int programmeCount = programmes.size();

        // Count assigned coordinators
        int assignedCoordinatorsCount = (int) programmes.stream()
                .filter(p -> (p.getCoordinator() != null && !p.getCoordinator().isBlank() && !"Unassigned".equalsIgnoreCase(p.getCoordinator()) && !"No coordinator assigned yet".equalsIgnoreCase(p.getCoordinator()) && !"Pending HOD Assignment".equalsIgnoreCase(p.getCoordinator())) || (p.getCoordinatorEmail() != null && !p.getCoordinatorEmail().isBlank()))
                .count();

        // Courses under department's programmes
        List<String> progIds = programmes.stream().map(Programme::getId).toList();
        int courseCount = 0;
        if (!progIds.isEmpty()) {
            courseCount = courseRepository.findByProgrammeIdIn(progIds).size();
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

        List<String> ALL_STEPS = List.of("coordinators", "batch", "outcomes", "review");
        List<String> completed = ALL_STEPS.stream().filter(existingCompleted::contains).toList();
        List<String> pending = ALL_STEPS.stream().filter(s -> !existingCompleted.contains(s)).toList();

        int currentStep;
        if (completed.size() == ALL_STEPS.size()) {
            currentStep = 1;
        } else if (targetStep != null && targetStep >= 1 && targetStep <= 4) {
            currentStep = (targetStep < 4 ? targetStep + 1 : 1);
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
            case "1", "coordinator", "coordinators", "programme_coordinator", "programme_coordinators" -> "coordinators";
            case "2", "batch", "batches", "batch_setup" -> "batch";
            case "3", "outcome", "outcomes", "po_pso", "po_pso_peo", "pos" -> "outcomes";
            case "4", "review", "confirm", "review_confirm" -> "review";
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
                .pendingSteps("coordinators,batch,outcomes,review")
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

        if (stepNumber == null || stepNumber < 1 || stepNumber > 4) {
            throw new IllegalArgumentException(
                    "Step number must be between 1 and 4"
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
                "coordinators,batch,outcomes,review"
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
        if (scope != null && scope.isProgrammeCoordinator()) {
            if (programmeId != null && !programmeId.isBlank()) {
                if (!programmeId.trim().equals(scope.getRequiredProgrammeId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your assigned scope.");
                }
            }
            return scope.getRequiredProgrammeId();
        }
        if (programmeId != null && !programmeId.isBlank()) {
            return programmeId.trim();
        }
        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            String emailTrim = coordinatorEmail.trim().toLowerCase();
            List<Programme> list = programmeRepository.findAll();
            Programme p = list.stream().filter(pr -> (pr.getCoordinatorEmail() != null && emailTrim.equalsIgnoreCase(pr.getCoordinatorEmail().trim())) || (pr.getCoordinator() != null && emailTrim.equalsIgnoreCase(pr.getCoordinator().trim()))).findFirst().orElse(null);
            if (p != null) return p.getId();
        }
        if (scope != null && scope.getProgrammeId() != null) {
            return scope.getProgrammeId();
        }
        return null;
    }

    // --- Programme Coordinator Summary & Setup Progress ---
    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSummaryDto getProgrammeCoordinatorSummary(String coordinatorEmail, String programmeId) {
        System.out.println("[AcademicService] getProgrammeCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail + " | programmeId: " + programmeId);

        String targetProgId = resolveTargetProgId(programmeId, coordinatorEmail);
        if (targetProgId == null || targetProgId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        Programme prog = programmeRepository.findById(targetProgId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + targetProgId));
        enrichProgrammeCoordinator(prog);

        List<Programme> assignedProgrammes = List.of(prog);
        CurrentUserScope scope = getScope();
        if (scope != null && (scope.isAdmin() || scope.isIqac())) {
            assignedProgrammes = programmeRepository.findAll();
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        } else if (scope != null && scope.isHod()) {
            assignedProgrammes = programmeRepository.findByDepartmentId(scope.getRequiredDepartmentId());
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        } else if (scope != null && scope.isDirector()) {
            assignedProgrammes = getProgrammesBySchool(scope.getRequiredSchoolId());
            assignedProgrammes.forEach(this::enrichProgrammeCoordinator);
        }

        String resolvedName = "Programme Coordinator";
        String resolvedEmail = coordinatorEmail != null ? coordinatorEmail : "";

        if (prog.getCoordinator() != null && !prog.getCoordinator().isBlank()) {
            resolvedName = prog.getCoordinator();
        }
        if (resolvedEmail.isBlank() && prog.getCoordinatorEmail() != null && !prog.getCoordinatorEmail().isBlank()) {
            resolvedEmail = prog.getCoordinatorEmail();
        }

        List<Course> courses = courseRepository.findByProgrammeId(targetProgId);
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeId(targetProgId);
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeId(targetProgId);
        List<PeoOutcome> peos = peoOutcomeRepository.findByProgrammeId(targetProgId);
        List<Batch> batches = batchRepository.findByProgrammeId(targetProgId);

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        String targetBatchId = batchId;
        if (targetBatchId == null || targetBatchId.isBlank()) {
            List<Batch> batches = batchRepository.findByProgrammeId(targetProgId);
            targetBatchId = !batches.isEmpty() ? batches.get(0).getId() : null;
        }

        final String finalProgId = targetProgId;
        final String finalBatchId = targetBatchId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeIdAndBatchId(finalProgId, finalBatchId)
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme scope cannot be determined.");
        }
        enforceProgrammeScope(targetProgId);

        String targetBatchId = batchId;
        if (targetBatchId == null || targetBatchId.isBlank()) {
            List<Batch> batches = batchRepository.findByProgrammeId(targetProgId);
            targetBatchId = !batches.isEmpty() ? batches.get(0).getId() : "batch-" + targetProgId;
        }

        final String finalProgId = targetProgId;
        final String finalBatchId = targetBatchId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeIdAndBatchId(finalProgId, finalBatchId)
                .orElseGet(() -> createDefaultPcProgress(finalProgId, finalBatchId, coordinatorEmail));

        progress.setBatchId(finalBatchId);

        int step = stepNumber != null ? stepNumber : progress.getCurrentStep();
        progress.setCurrentStep(step);
        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            progress.setCoordinatorEmail(coordinatorEmail);
        }

        Set<String> completedSet = new LinkedHashSet<>();
        if (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()) {
            for (String s : progress.getCompletedSteps().split(",")) {
                String clean = s.trim();
                if (!clean.isEmpty()) completedSet.add(clean);
            }
        }

        if (body != null && body.containsKey("completedSteps")) {
            Object csObj = body.get("completedSteps");
            if (csObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) completedSet.add(item.toString().trim());
                }
            } else if (csObj instanceof String csStr) {
                for (String s : csStr.split(",")) {
                    String clean = s.trim();
                    if (!clean.isEmpty()) completedSet.add(clean);
                }
            }
        } else if (stepNumber != null) {
            completedSet.add(String.valueOf(stepNumber));
            if (stepNumber == 0 || stepNumber == 1) completedSet.add("programme setup");
            if (stepNumber == 1 || stepNumber == 2) completedSet.add("po/pso target");
            if (stepNumber == 2 || stepNumber == 3) completedSet.add("programme atr");
            if (stepNumber == 3 || stepNumber == 4) completedSet.add("verify&finish");
        }

        progress.setCompletedSteps(String.join(",", completedSet));

        Set<String> allSteps = new LinkedHashSet<>(List.of("0", "1", "2", "3"));
        allSteps.removeAll(completedSet);
        progress.setPendingSteps(String.join(",", allSteps));
        progress.setUpdatedAt(ZonedDateTime.now());

        if (completedSet.contains("3") || completedSet.contains("4") || completedSet.contains("verify&finish")) {
            progress.setOverallStatus(SetupStepStatus.COMPLETED);
        } else if (!completedSet.isEmpty()) {
            progress.setOverallStatus(SetupStepStatus.IN_PROGRESS);
        }

        pcSetupProgressRepository.save(progress);
        return buildPcSetupProgressDto(progress);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto updateProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId, Integer stepNumber) {
        return updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, null, stepNumber, null);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto completeProgrammeCoordinatorSetup(String coordinatorEmail, String programmeId, String batchId) {
        System.out.println("[AcademicService] completeProgrammeCoordinatorSetup called | programmeId: " + programmeId + " | batchId: " + batchId + " | coordinatorEmail: " + coordinatorEmail);
        Map<String, Object> body = Map.of("completedSteps", List.of("0", "1", "2", "3"));
        return updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, batchId, 0, body);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto completeProgrammeCoordinatorSetup(String coordinatorEmail, String programmeId) {
        return completeProgrammeCoordinatorSetup(coordinatorEmail, programmeId, null);
    }

    private ProgrammeCoordinatorSetupProgress createDefaultPcProgress(String programmeId, String batchId, String coordinatorEmail) {
        String finalBatchId = batchId;
        if (finalBatchId == null || finalBatchId.isBlank()) {
            List<Batch> batches = batchRepository.findByProgrammeId(programmeId);
            finalBatchId = !batches.isEmpty() ? batches.get(0).getId() : "batch-" + programmeId;
        }
        return ProgrammeCoordinatorSetupProgress.builder()
                .id("pcprog-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeId(programmeId)
                .batchId(finalBatchId)
                .coordinatorEmail(coordinatorEmail)
                .currentStep(0)
                .overallStatus(SetupStepStatus.IN_PROGRESS)
                .completedSteps("")
                .pendingSteps("0,1,2,3")
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

        return ProgrammeCoordinatorSetupProgressDto.builder()
                .id(progress.getId())
                .programmeId(progress.getProgrammeId())
                .batchId(progress.getBatchId())
                .coordinatorEmail(progress.getCoordinatorEmail())
                .currentStep(progress.getCurrentStep())
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
        List<Programme> progs = (targetDeptId != null && !targetDeptId.isBlank())
                ? programmeRepository.findByDepartmentId(targetDeptId)
                : getAllProgrammes();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Programme p : progs) {
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
            Programme p = programmeRepository.findById(progId).orElse(null);
            if (p != null) {
                p.setCoordinator(name);
                p.setCoordinatorEmail(email);
                saveProgramme(p);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Programme coordinator assigned successfully.");
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
        }
        if (allocations != null) {
            for (Map<String, Object> item : allocations) {
                String courseId = item.get("courseId") != null ? item.get("courseId").toString() : null;
                String email = item.get("coordinatorEmail") != null ? item.get("coordinatorEmail").toString() : "";
                String name = item.get("courseCoordinatorName") != null ? item.get("courseCoordinatorName").toString() : (item.get("coordinator") != null ? item.get("coordinator").toString() : "");

                if (courseId != null) {
                    enforceCourseScope(courseId);
                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course != null) {
                        course.setCoordinator(name);
                        course.setFaculty(name);
                        course.setAssignedFaculty(name + " (" + email + ")");
                        courseRepository.save(course);
                    }

                    if (batchId != null && !batchId.isBlank()) {
                        // Resolve the coordinator user
                        User coordinatorUser = null;
                        if (!email.isBlank()) {
                            coordinatorUser = userRepository.findByEmail(email).orElse(null);
                        }
                        
                        // Check if CourseOffering already exists for courseId + batchId
                        List<CourseOffering> existingOfferings = courseOfferingRepository.findByCourseId(courseId);
                        CourseOffering targetOffering = existingOfferings.stream()
                                .filter(o -> batchId.equals(o.getBatchId()))
                                .findFirst()
                                .orElse(null);
                                
                        String semStr = item.get("semester") != null ? item.get("semester").toString() : (course != null ? course.getSemester() : null);
                        Integer parsedSem = (semStr != null && semStr.trim().matches("\\d+")) ? Integer.parseInt(semStr.trim()) : 1;

                        if (targetOffering == null) {
                            // Create exactly one CourseOffering
                            targetOffering = CourseOffering.builder()
                                    .id("off-" + UUID.randomUUID().toString().substring(0, 8))
                                    .courseId(courseId)
                                    .batchId(batchId)
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
                        
                        courseOfferingRepository.save(targetOffering);
                    } else {
                        // Fallback to update existing offerings if batchId is not provided
                        String semStr = item.get("semester") != null ? item.get("semester").toString() : (course != null ? course.getSemester() : null);
                        Integer parsedSem = (semStr != null && semStr.trim().matches("\\d+")) ? Integer.parseInt(semStr.trim()) : null;
                        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
                        for (CourseOffering off : offerings) {
                            off.setCourseCoordinatorName(name);
                            off.setAssignedFaculty(name + " (" + email + ")");
                            if (parsedSem != null) {
                                off.setSemester(parsedSem);
                            }
                            if (!email.isBlank()) {
                                userRepository.findByEmail(email).ifPresent(u -> off.setCourseCoordinatorId(u.getId()));
                            }
                            courseOfferingRepository.save(off);
                        }
                    }
                }
            }
        }

        if (submit) {
            ApprovalRequest req = ApprovalRequest.builder()
                    .id("app-alloc-" + UUID.randomUUID().toString().substring(0, 8))
                    .type(ApprovalType.COURSE_ALLOCATION)
                    .title("Course Allocation for Programme " + programmeId)
                    .programmeId(programmeId)
                    .resourceId("allocation-" + programmeId)
                    .status(ApprovalStatus.PENDING)
                    .submittedBy("Programme Coordinator")
                    .submittedAt(ZonedDateTime.now())
                    .remarks("Allocations submitted for HOD review.")
                    .build();
            approvalRequestRepository.save(req);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", submit ? "Course allocations saved and submitted for verification." : "Course allocations saved successfully.");
        return res;
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
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);
        List<PeoOutcome> peos = peoOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("programmeId", pId);
        data.put("batchId", batchId);
        data.put("pos", pos);
        data.put("psos", psos);
        data.put("peos", peos);
        return data;
    }

    @Transactional
    public Map<String, Object> saveConsolidatedOutcomes(Map<String, Object> payload) {
        String progId = payload != null && payload.get("programmeId") != null ? payload.get("programmeId").toString().trim() : null;
        if (progId == null || progId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme ID is required to save outcomes.");
        }
        enforceProgrammeScope(progId);

        // 1. Process and save POs
        if (payload != null && payload.get("pos") instanceof List<?> poList) {
            List<ProgrammeOutcome> existingPOs = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPOs.isEmpty()) {
                programmeOutcomeRepository.deleteAll(existingPOs);
                programmeOutcomeRepository.flush();
            }

            for (Object obj : poList) {
                if (obj instanceof Map<?, ?> poMap) {
                    String code = poMap.get("code") != null ? poMap.get("code").toString() : null;
                    String statement = poMap.get("statement") != null ? poMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        ProgrammeOutcome po = ProgrammeOutcome.builder()
                                .id("po-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        programmeOutcomeRepository.save(po);
                    }
                }
            }
        }

        // 2. Process and save PSOs
        if (payload != null && payload.get("psos") instanceof List<?> psoList) {
            List<ProgrammeSpecificOutcome> existingPSOs = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPSOs.isEmpty()) {
                programmeSpecificOutcomeRepository.deleteAll(existingPSOs);
                programmeSpecificOutcomeRepository.flush();
            }

            for (Object obj : psoList) {
                if (obj instanceof Map<?, ?> psoMap) {
                    String code = psoMap.get("code") != null ? psoMap.get("code").toString() : null;
                    String statement = psoMap.get("statement") != null ? psoMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        ProgrammeSpecificOutcome pso = ProgrammeSpecificOutcome.builder()
                                .id("pso-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        programmeSpecificOutcomeRepository.save(pso);
                    }
                }
            }
        }

        // 3. Process and save PEOs
        if (payload != null && payload.get("peos") instanceof List<?> peoList) {
            List<PeoOutcome> existingPEOs = peoOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPEOs.isEmpty()) {
                peoOutcomeRepository.deleteAll(existingPEOs);
                peoOutcomeRepository.flush();
            }

            for (Object obj : peoList) {
                if (obj instanceof Map<?, ?> peoMap) {
                    String code = peoMap.get("code") != null ? peoMap.get("code").toString() : null;
                    String statement = peoMap.get("statement") != null ? peoMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        PeoOutcome peo = PeoOutcome.builder()
                                .id("peo-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
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
        res.put("data", getConsolidatedOutcomes(progId, null));
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
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);

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
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);

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
