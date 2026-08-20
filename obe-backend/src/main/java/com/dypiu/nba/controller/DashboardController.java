package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.ReportAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AcademicService academicService;
    private final ReportAccessService reportAccessService;
    private final CurrentUserScopeService currentUserScopeService;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final UserRepository userRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AttainmentConfigurationRepository configRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final StudentCoMarkRepository studentCoMarkRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;

    @GetMapping("/director")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDirectorDashboard(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String directorEmail,
            Principal principal) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);
        String targetSchoolId = null;

        if (scope.isDirector()) {
            targetSchoolId = scope.getRequiredSchoolId();
        } else if (scope.isAdmin() || scope.isIqac()) {
            if (schoolId != null && !schoolId.isBlank()) {
                targetSchoolId = schoolId.trim();
            } else if (directorEmail != null && !directorEmail.isBlank()) {
                targetSchoolId = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail.trim())
                        .map(School::getId).orElse(null);
            }
            if (targetSchoolId == null) {
                targetSchoolId = scope.getSchoolId();
            }
        } else if (scope.getSchoolId() != null) {
            targetSchoolId = scope.getSchoolId();
        }

        if (targetSchoolId == null || targetSchoolId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School scope cannot be determined for Director dashboard.");
        }

        final String finalSchoolId = targetSchoolId;
        School school = schoolRepository.findById(finalSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found: " + finalSchoolId));

        List<Department> depts = departmentRepository.findBySchoolId(finalSchoolId);
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        List<Programme> progs = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);
        List<String> progIds = progs.stream().map(Programme::getId).toList();
        List<Batch> activeBatches = progIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(progIds).stream()
                .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        String targetEmail = school.getDirectorEmail() != null && !school.getDirectorEmail().isBlank()
                ? school.getDirectorEmail()
                : (scope.getEmail() != null ? scope.getEmail() : directorEmail);
        DirectorSetupProgressDto progress = academicService.getDirectorSetupProgress(finalSchoolId, targetEmail);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("departments", depts.size());
        stats.put("departmentsCount", depts.size());
        stats.put("programmes", progs.size());
        stats.put("programmesCount", progs.size());
        stats.put("activeBatches", activeBatches.size());
        stats.put("activeBatchesCount", activeBatches.size());

        Map<String, Boolean> workflowProgress = new LinkedHashMap<>();
        workflowProgress.put("1", !depts.isEmpty());
        workflowProgress.put("2", !progs.isEmpty());
        workflowProgress.put("3", !activeBatches.isEmpty());
        workflowProgress.put("4", progress != null && progress.getOverallStatus() == SetupStepStatus.COMPLETED);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("school", school);
        data.put("setupProgress", progress);
        data.put("workflowProgress", workflowProgress);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }

    @GetMapping("/hod")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHodDashboard(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail,
            Principal principal) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);
        String targetDeptId = null;
        String targetSchoolId = null;

        if (scope.isHod()) {
            targetSchoolId = scope.getRequiredSchoolId();
            targetDeptId = scope.getRequiredDepartmentId();
        } else if (scope.isAdmin() || scope.isIqac()) {
            if (departmentId != null && !departmentId.isBlank()) {
                targetDeptId = departmentId.trim();
            } else if (hodEmail != null && !hodEmail.isBlank()) {
                List<Department> depts = departmentRepository.findByHodEmailIgnoreCase(hodEmail.trim());
                if (!depts.isEmpty()) targetDeptId = depts.get(0).getId();
            }
            if (targetDeptId == null) {
                targetDeptId = scope.getDepartmentId();
            }
            targetSchoolId = scope.getSchoolId();
        } else {
            targetSchoolId = scope.getSchoolId();
            targetDeptId = scope.getDepartmentId();
            if (targetDeptId == null && departmentId != null && !departmentId.isBlank()) {
                targetDeptId = departmentId.trim();
            }
        }

        if (targetDeptId == null || targetDeptId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department scope cannot be determined for HOD dashboard.");
        }

        final String finalDeptId = targetDeptId;
        Department primaryDept = departmentRepository.findById(finalDeptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found: " + finalDeptId));

        if (targetSchoolId != null && primaryDept.getSchoolId() != null && !primaryDept.getSchoolId().equals(targetSchoolId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Department does not belong to your school scope.");
        }

        List<Department> matchedDepts = List.of(primaryDept);
        List<Programme> progs = programmeRepository.findByDepartmentId(finalDeptId);
        Set<String> progIds = progs.stream().map(Programme::getId).collect(Collectors.toSet());
        List<Batch> activeBatches = progIds.isEmpty() ? Collections.emptyList() : batchRepository.findByProgrammeIdIn(progIds).stream()
                .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
        Set<String> batchIds = activeBatches.stream().map(Batch::getId).collect(Collectors.toSet());
        List<CourseOffering> offerings = batchIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(batchIds);
        List<Course> courses = progIds.isEmpty() ? Collections.emptyList() : courseRepository.findByProgrammeIdIn(new ArrayList<>(progIds));

        long allocationsPending = progIds.isEmpty() ? 0 : approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && progIds.contains(a.getProgrammeId()) && a.getStatus() == ApprovalStatus.PENDING)
                .count();
        long targetsPending = progIds.isEmpty() ? 0 : approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.PO_PSO_TARGETS && progIds.contains(a.getProgrammeId()) && a.getStatus() == ApprovalStatus.PENDING)
                .count();
        long programmeAtrPending = progIds.isEmpty() ? 0 : programmeAtrRepository.findAll().stream()
                .filter(p -> progIds.contains(p.getProgrammeId()) && p.getStatus() == ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION)
                .count();
        long pendingApprovalsCount = allocationsPending + targetsPending + programmeAtrPending;

        Map<String, Object> pendingBreakdown = new LinkedHashMap<>();
        pendingBreakdown.put("allocationsPending", allocationsPending);
        pendingBreakdown.put("targetsPending", targetsPending);
        pendingBreakdown.put("programmeAtrPending", programmeAtrPending);

        String targetEmail = primaryDept.getHodEmail() != null && !primaryDept.getHodEmail().isBlank()
                ? primaryDept.getHodEmail()
                : (scope.getEmail() != null ? scope.getEmail() : hodEmail);
        HodSetupProgressDto progress = academicService.getHodSetupProgress(finalDeptId, targetEmail);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("programmes", progs.size());
        stats.put("programmesCount", progs.size());
        stats.put("coursesCount", courses.size());
        stats.put("activeBatches", activeBatches.size());
        stats.put("activeBatchesCount", activeBatches.size());
        stats.put("courseOfferings", offerings.size());
        stats.put("pendingApprovalsCount", pendingApprovalsCount);
        stats.put("pendingBreakdown", pendingBreakdown);

        Batch activeBatch = activeBatches.isEmpty() ? null : activeBatches.get(0);

        Map<String, Boolean> workflowProgress = new LinkedHashMap<>();
        workflowProgress.put("1", !progs.isEmpty());
        workflowProgress.put("2", !offerings.isEmpty());
        workflowProgress.put("3", !activeBatches.isEmpty());
        workflowProgress.put("4", progress != null && progress.getOverallStatus() == SetupStepStatus.COMPLETED);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("department", primaryDept);
        data.put("departments", matchedDepts);
        data.put("setupProgress", progress);
        data.put("activeBatch", activeBatch != null ? activeBatch.getName() : "");
        data.put("workflowProgress", workflowProgress);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }

    @GetMapping("/programme-coordinator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgrammeCoordinatorDashboard(
            @RequestParam(required = false) String programmeId,
            Principal principal) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);
        String targetProgId = null;

        if (scope.isProgrammeCoordinator()) {
            if (programmeId != null && !programmeId.isBlank()) {
                if (!programmeId.trim().equals(scope.getRequiredProgrammeId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your assigned scope.");
                }
            }
            targetProgId = scope.getRequiredProgrammeId();
        } else if (scope.isAdmin() || scope.isIqac()) {
            if (programmeId != null && !programmeId.isBlank()) {
                targetProgId = programmeId.trim();
            } else if (scope.getProgrammeId() != null) {
                targetProgId = scope.getProgrammeId();
            }
        } else if (scope.isHod()) {
            if (programmeId != null && !programmeId.isBlank()) {
                targetProgId = programmeId.trim();
            }
        } else if (scope.isDirector()) {
            if (programmeId != null && !programmeId.isBlank()) {
                targetProgId = programmeId.trim();
            }
        } else if (scope.getProgrammeId() != null) {
            targetProgId = scope.getProgrammeId();
        } else if (programmeId != null && !programmeId.isBlank()) {
            targetProgId = programmeId.trim();
        }

        if (targetProgId == null || targetProgId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme scope cannot be determined for Programme Coordinator dashboard.");
        }

        final String finalProgId = targetProgId;
        Programme prog = programmeRepository.findById(finalProgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme not found: " + finalProgId));

        if (scope.isProgrammeCoordinator()) {
            if (scope.hasDepartmentScope() && prog.getDepartmentId() != null && !prog.getDepartmentId().equals(scope.getRequiredDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme does not belong to your assigned department.");
            }
            if (scope.hasSchoolScope() && prog.getDepartmentId() != null) {
                Department dept = departmentRepository.findById(prog.getDepartmentId()).orElse(null);
                if (dept != null && dept.getSchoolId() != null && !dept.getSchoolId().equals(scope.getRequiredSchoolId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme does not belong to your assigned school.");
                }
            }
        } else if (scope.isHod()) {
            if (prog.getDepartmentId() != null && !prog.getDepartmentId().equals(scope.getRequiredDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme does not belong to your assigned department.");
            }
        } else if (scope.isDirector()) {
            if (prog.getDepartmentId() != null) {
                Department dept = departmentRepository.findById(prog.getDepartmentId()).orElse(null);
                if (dept != null && dept.getSchoolId() != null && !dept.getSchoolId().equals(scope.getRequiredSchoolId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme does not belong to your assigned school.");
                }
            }
        }

        List<Batch> batches = batchRepository.findByProgrammeId(finalProgId);
        Set<String> batchIds = batches.stream().map(Batch::getId).collect(Collectors.toSet());
        List<CourseOffering> offerings = batchIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(batchIds);
        List<Course> courses = courseRepository.findByProgrammeId(finalProgId);

        List<String> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());
        long configPending = offeringIds.isEmpty() ? 0 : configRepository.findAll().stream()
                .filter(c -> offeringIds.contains(c.getCourseOfferingId()) && (c.getStatus() == AttainmentConfigStatus.DRAFT || c.getStatus() == null))
                .count();
        long coTargetsPending = offeringIds.isEmpty() ? 0 : approvalRequestRepository.findAll().stream()
                .filter(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && offeringIds.contains(a.getCourseOfferingId()) && a.getStatus() == ApprovalStatus.PENDING)
                .count();
        long courseAtrPending = offeringIds.isEmpty() ? 0 : courseAtrRepository.findByCourseOfferingIdIn(offeringIds).stream()
                .filter(a -> a.getStatus() == CourseAtrStatus.SUBMITTED_FOR_VERIFICATION)
                .count();
        long pendingVerifications = configPending + coTargetsPending + courseAtrPending;

        Map<String, Object> pendingBreakdown = new LinkedHashMap<>();
        pendingBreakdown.put("configPending", configPending);
        pendingBreakdown.put("coTargetsPending", coTargetsPending);
        pendingBreakdown.put("courseAtrPending", courseAtrPending);

        String targetEmail = prog.getCoordinatorEmail() != null && !prog.getCoordinatorEmail().isBlank()
                ? prog.getCoordinatorEmail()
                : (scope.getEmail() != null ? scope.getEmail() : null);
        ProgrammeCoordinatorSetupProgressDto progress = academicService.getProgrammeCoordinatorSetupProgress(targetEmail, finalProgId);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courses", courses.size());
        stats.put("coursesCount", courses.size());
        stats.put("courseOfferings", offerings.size());
        stats.put("pendingCourseAtrApprovals", courseAtrPending);
        stats.put("pendingVerifications", pendingVerifications);
        stats.put("pendingBreakdown", pendingBreakdown);

        Batch activeBatch = batches.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).findFirst().orElse(batches.isEmpty() ? null : batches.get(0));

        Map<String, Boolean> workflowProgress = new LinkedHashMap<>();
        workflowProgress.put("1", !courses.isEmpty());
        workflowProgress.put("2", !offerings.isEmpty());
        workflowProgress.put("3", !batches.isEmpty());
        workflowProgress.put("4", progress != null && progress.getOverallStatus() == SetupStepStatus.COMPLETED);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("programmeId", prog.getId());
        data.put("programme", prog);
        data.put("setupProgress", progress);
        data.put("batches", batches);
        data.put("activeBatch", activeBatch != null ? activeBatch.getName() : "");
        data.put("workflowProgress", workflowProgress);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }

    @GetMapping({"/course-coordinator", "/faculty"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoordinatorDashboard(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);

        List<CourseOffering> allOfferings = courseOfferingRepository.findAll();
        List<CourseOffering> assignedOfferings = allOfferings.stream()
                .filter(o -> user != null && o.getCourseCoordinatorId() != null && java.util.Objects.equals(o.getCourseCoordinatorId(), user.getId()))
                .collect(Collectors.toList());

        CourseOffering targetOffering = null;
        if (courseId != null && !courseId.isBlank()) {
            if (scope != null && scope.isFaculty()) {
                boolean assignedToCourse = assignedOfferings.stream().anyMatch(o -> 
                        (o.getCourseId() != null && o.getCourseId().equalsIgnoreCase(courseId.trim())) || 
                        (o.getId() != null && o.getId().equalsIgnoreCase(courseId.trim())));
                if (!assignedToCourse) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course / Course Offering.");
                }
            }
            targetOffering = courseOfferingRepository.findByCourseId(courseId.trim()).stream()
                    .filter(o -> scope == null || !scope.isFaculty() || assignedOfferings.contains(o))
                    .findFirst()
                    .orElse(null);
            if (targetOffering == null) {
                targetOffering = courseOfferingRepository.findById(courseId.trim()).orElse(null);
            }
        }
        if (targetOffering == null && !assignedOfferings.isEmpty()) {
            targetOffering = assignedOfferings.get(0);
        }

        Course course = null;
        String offeringId = targetOffering != null ? targetOffering.getId() : null;
        String targetCrsId = targetOffering != null ? targetOffering.getCourseId() : courseId;
        if (targetCrsId != null && !targetCrsId.isBlank()) {
            course = courseRepository.findById(targetCrsId).orElse(null);
        }

        List<CourseOutcome> cos = (offeringId != null) ? courseOutcomeRepository.findByCourseOfferingId(offeringId) : Collections.emptyList();
        List<String> coIds = cos.stream().map(CourseOutcome::getId).toList();

        boolean outcomesDone = !cos.isEmpty();
        boolean targetsDone = outcomesDone && cos.stream().allMatch(c -> c.getTargetLevel() != null);
        boolean mappingDone = !coIds.isEmpty() && (!coPoMappingRepository.findByCourseOutcomeIdIn(coIds).isEmpty() || !coPsoMappingRepository.findByCourseOutcomeIdIn(coIds).isEmpty());
        boolean configDone = (offeringId != null) && configRepository.findByCourseOfferingId(offeringId).isPresent();
        boolean marksDone = (offeringId != null) && (!studentCoMarkRepository.findByCourseOfferingId(offeringId).isEmpty() || !uploadedDocumentRepository.findByCourseOfferingId(offeringId).isEmpty());
        boolean atrDone = (offeringId != null) && !courseAtrRepository.findByCourseOfferingId(offeringId).isEmpty();

        Map<String, Boolean> workflowProgress = new LinkedHashMap<>();
        workflowProgress.put("/outcomes", outcomesDone);
        workflowProgress.put("/co-targets", targetsDone);
        workflowProgress.put("/co-mapping", mappingDone);
        workflowProgress.put("/attainment-config", configDone);
        workflowProgress.put("/marks-upload", marksDone);
        workflowProgress.put("/course-atr", atrDone);

        boolean isConfigRevision = (offeringId != null) && approvalRequestRepository.findAll().stream()
                .anyMatch(a -> a.getType() == ApprovalType.ATTAINMENT_CONFIGURATION && offeringId.equalsIgnoreCase(a.getCourseOfferingId()) && a.getStatus() == ApprovalStatus.NEEDS_REVISION);
        boolean isCoRevision = (offeringId != null) && approvalRequestRepository.findAll().stream()
                .anyMatch(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && offeringId.equalsIgnoreCase(a.getCourseOfferingId()) && a.getStatus() == ApprovalStatus.NEEDS_REVISION);
        boolean isAtrRevision = (offeringId != null) && courseAtrRepository.findByCourseOfferingId(offeringId).stream()
                .anyMatch(a -> a.getStatus() == CourseAtrStatus.NEEDS_REVISION);
        boolean hasRevision = isConfigRevision || isCoRevision || isAtrRevision;

        Map<String, Boolean> revisions = new LinkedHashMap<>();
        revisions.put("hasRevision", hasRevision);
        revisions.put("isConfigRevision", isConfigRevision);
        revisions.put("isCoRevision", isCoRevision);
        revisions.put("isAtrRevision", isAtrRevision);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assignedCourseOfferingsCount", assignedOfferings.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("course", course);
        data.put("workflowProgress", workflowProgress);
        data.put("revisions", revisions);
        data.put("assignedCourseOfferings", assignedOfferings);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }
}
