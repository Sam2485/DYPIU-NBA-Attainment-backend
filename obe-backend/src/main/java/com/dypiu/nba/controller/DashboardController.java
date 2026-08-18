package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.ReportAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AcademicService academicService;
    private final ReportAccessService reportAccessService;
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
        User user = reportAccessService.getAuthenticatedUser(principal);
        String targetEmail = directorEmail != null && !directorEmail.isBlank() ? directorEmail : (user != null ? user.getEmail() : null);
        String sId = schoolId != null ? schoolId : (user != null ? user.getSchoolId() : null);

        School school = null;
        if (targetEmail != null && !targetEmail.isBlank()) {
            school = schoolRepository.findByDirectorEmailIgnoreCase(targetEmail.trim()).orElse(null);
        }
        if (school == null && sId != null && !sId.isBlank()) {
            school = schoolRepository.findById(sId).orElse(null);
        }
        if (school == null && user != null && user.getId() != null) {
            school = schoolRepository.findByDirectorId(user.getId()).orElse(null);
        }

        String targetSchoolId = school != null ? school.getId() : (sId != null ? sId : "sch-1");

        List<Department> depts = departmentRepository.findBySchoolId(targetSchoolId);
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        List<Programme> progs = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);
        List<String> progIds = progs.stream().map(Programme::getId).toList();
        List<Batch> activeBatches = progIds.isEmpty() ? Collections.emptyList() : batchRepository.findAll().stream()
                .filter(b -> progIds.contains(b.getProgrammeId()) && "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());

        DirectorSetupProgressDto progress = academicService.getDirectorSetupProgress(targetSchoolId, targetEmail);

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
        User user = reportAccessService.getAuthenticatedUser(principal);
        String targetEmail = hodEmail != null && !hodEmail.isBlank() ? hodEmail : (user != null ? user.getEmail() : null);
        String dId = departmentId != null ? departmentId : (user != null ? user.getDepartmentId() : null);

        Department dept = null;
        if (targetEmail != null && !targetEmail.isBlank()) {
            dept = departmentRepository.findByHodEmailIgnoreCase(targetEmail.trim()).orElse(null);
        }
        if (dept == null && dId != null && !dId.isBlank()) {
            dept = departmentRepository.findById(dId).orElse(null);
        }
        if (dept == null && targetEmail != null && !targetEmail.isBlank()) {
            User u = userRepository.findByEmail(targetEmail.trim()).orElse(null);
            if (u != null && u.getDepartment() != null && !u.getDepartment().isBlank()) {
                dept = departmentRepository.findByName(u.getDepartment().trim()).orElse(null);
            }
        }
        if (dept == null) {
            dept = departmentRepository.findAll().stream().findFirst().orElse(null);
        }

        String targetDeptId = dept != null ? dept.getId() : (dId != null ? dId : "dept-1");

        List<Programme> progs = programmeRepository.findByDepartmentId(targetDeptId);
        Set<String> progIds = progs.stream().map(Programme::getId).collect(Collectors.toSet());
        List<Batch> activeBatches = progIds.isEmpty() ? Collections.emptyList() : batchRepository.findAll().stream()
                .filter(b -> progIds.contains(b.getProgrammeId()) && "ACTIVE".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
        Set<String> batchIds = activeBatches.stream().map(Batch::getId).collect(Collectors.toSet());
        List<CourseOffering> offerings = batchIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(batchIds);
        List<Course> courses = progIds.isEmpty() ? Collections.emptyList() : courseRepository.findByProgrammeIdIn(new ArrayList<>(progIds));

        long allocationsPending = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.COURSE_ALLOCATION && progIds.contains(a.getProgrammeId()) && a.getStatus() == ApprovalStatus.PENDING)
                .count();
        long targetsPending = approvalRequestRepository.findAll().stream()
                .filter(a -> a.getType() == ApprovalType.PO_PSO_TARGETS && progIds.contains(a.getProgrammeId()) && a.getStatus() == ApprovalStatus.PENDING)
                .count();
        long programmeAtrPending = programmeAtrRepository.findAll().stream()
                .filter(p -> progIds.contains(p.getProgrammeId()) && p.getStatus() == ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION)
                .count();
        long pendingApprovalsCount = allocationsPending + targetsPending + programmeAtrPending;

        Map<String, Object> pendingBreakdown = new LinkedHashMap<>();
        pendingBreakdown.put("allocationsPending", allocationsPending);
        pendingBreakdown.put("targetsPending", targetsPending);
        pendingBreakdown.put("programmeAtrPending", programmeAtrPending);

        HodSetupProgressDto progress = academicService.getHodSetupProgress(targetDeptId, targetEmail);

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
        data.put("department", dept);
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
        User user = reportAccessService.getAuthenticatedUser(principal);
        String pId = programmeId != null ? programmeId : (user != null ? user.getProgrammeId() : null);

        Programme prog = pId != null ? programmeRepository.findById(pId).orElse(null) : programmeRepository.findAll().stream().findFirst().orElse(null);
        String targetProgId = prog != null ? prog.getId() : "prog-1";

        List<Batch> batches = batchRepository.findByProgrammeId(targetProgId);
        Set<String> batchIds = batches.stream().map(Batch::getId).collect(Collectors.toSet());
        List<CourseOffering> offerings = batchIds.isEmpty() ? Collections.emptyList() : courseOfferingRepository.findByBatchIdIn(batchIds);
        List<Course> courses = courseRepository.findByProgrammeId(targetProgId);

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

        ProgrammeCoordinatorSetupProgressDto progress = academicService.getProgrammeCoordinatorSetupProgress(user != null ? user.getEmail() : null, targetProgId);

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

        List<CourseOffering> assignedOfferings = courseOfferingRepository.findAll().stream()
                .filter(o -> user != null && ((o.getCourseCoordinatorId() != null && java.util.Objects.equals(o.getCourseCoordinatorId(), user.getId()))
                        || (o.getCourseCoordinatorName() != null && o.getCourseCoordinatorName().equalsIgnoreCase(user.getName()))
                        || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(user.getEmail()) || o.getAssignedFaculty().contains(user.getName())))))
                .collect(Collectors.toList());

        CourseOffering targetOffering = null;
        if (courseId != null && !courseId.isBlank()) {
            targetOffering = courseOfferingRepository.findByCourseId(courseId).stream().findFirst().orElse(null);
        }
        if (targetOffering == null && !assignedOfferings.isEmpty()) {
            targetOffering = assignedOfferings.get(0);
        }

        Course course = null;
        String offeringId = targetOffering != null ? targetOffering.getId() : (courseId != null ? courseId : "off-101");
        String targetCrsId = targetOffering != null ? targetOffering.getCourseId() : courseId;
        if (targetCrsId != null && !targetCrsId.isBlank()) {
            course = courseRepository.findById(targetCrsId).orElse(null);
        }
        if (course == null) {
            course = Course.builder().id(targetCrsId != null ? targetCrsId : "crs-1").code("310244").name("Computer Network & Security").credits(4).courseType("Theory").build();
        }

        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);
        List<String> coIds = cos.stream().map(CourseOutcome::getId).toList();

        boolean outcomesDone = !cos.isEmpty();
        boolean targetsDone = outcomesDone && cos.stream().allMatch(c -> c.getTargetLevel() != null);
        boolean mappingDone = !coIds.isEmpty() && (!coPoMappingRepository.findByCourseOutcomeIdIn(coIds).isEmpty() || !coPsoMappingRepository.findByCourseOutcomeIdIn(coIds).isEmpty());
        boolean configDone = configRepository.findByCourseOfferingId(offeringId).isPresent();
        boolean marksDone = !studentCoMarkRepository.findByCourseOfferingId(offeringId).isEmpty() || !uploadedDocumentRepository.findByCourseOfferingId(offeringId).isEmpty();
        boolean atrDone = !courseAtrRepository.findByCourseOfferingId(offeringId).isEmpty();

        Map<String, Boolean> workflowProgress = new LinkedHashMap<>();
        workflowProgress.put("/outcomes", outcomesDone);
        workflowProgress.put("/co-targets", targetsDone);
        workflowProgress.put("/co-mapping", mappingDone);
        workflowProgress.put("/attainment-config", configDone);
        workflowProgress.put("/marks-upload", marksDone);
        workflowProgress.put("/course-atr", atrDone);

        boolean isConfigRevision = approvalRequestRepository.findAll().stream()
                .anyMatch(a -> a.getType() == ApprovalType.ATTAINMENT_CONFIGURATION && offeringId.equalsIgnoreCase(a.getCourseOfferingId()) && a.getStatus() == ApprovalStatus.NEEDS_REVISION);
        boolean isCoRevision = approvalRequestRepository.findAll().stream()
                .anyMatch(a -> (a.getType() == ApprovalType.CO_DEFINITION || a.getType() == ApprovalType.CO_TARGETS) && offeringId.equalsIgnoreCase(a.getCourseOfferingId()) && a.getStatus() == ApprovalStatus.NEEDS_REVISION);
        boolean isAtrRevision = courseAtrRepository.findByCourseOfferingId(offeringId).stream()
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
