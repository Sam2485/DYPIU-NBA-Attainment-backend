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
        stats.put("programmes", progs.size());
        stats.put("activeBatches", activeBatches.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("school", school);
        data.put("setupProgress", progress);
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

        HodSetupProgressDto progress = academicService.getHodSetupProgress(targetDeptId, targetEmail);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("programmes", progs.size());
        stats.put("activeBatches", activeBatches.size());
        stats.put("courseOfferings", offerings.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("department", dept);
        data.put("setupProgress", progress);
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
        List<CourseOffering> offerings = courseOfferingRepository.findByBatchIdIn(batchIds);
        List<Course> courses = courseRepository.findByProgrammeId(targetProgId);

        List<String> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());
        long pendingCourseAtrs = offeringIds.isEmpty() ? 0 : courseAtrRepository.findByCourseOfferingIdIn(offeringIds).stream()
                .filter(a -> a.getStatus() == CourseAtrStatus.SUBMITTED_FOR_VERIFICATION)
                .count();

        ProgrammeCoordinatorSetupProgressDto progress = academicService.getProgrammeCoordinatorSetupProgress(user != null ? user.getEmail() : null, targetProgId);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courses", courses.size());
        stats.put("courseOfferings", offerings.size());
        stats.put("pendingCourseAtrApprovals", pendingCourseAtrs);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("programme", prog);
        data.put("setupProgress", progress);
        data.put("batches", batches);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }

    @GetMapping("/course-coordinator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoordinatorDashboard(Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);

        List<CourseOffering> assignedOfferings = courseOfferingRepository.findAll().stream()
                .filter(o -> user != null && ((o.getCourseCoordinatorId() != null && java.util.Objects.equals(o.getCourseCoordinatorId(), user.getId()))
                        || (o.getCourseCoordinatorName() != null && o.getCourseCoordinatorName().equalsIgnoreCase(user.getName()))
                        || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(user.getEmail()) || o.getAssignedFaculty().contains(user.getName())))))
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assignedCourseOfferingsCount", assignedOfferings.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assignedCourseOfferings", assignedOfferings);
        data.put("statistics", stats);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }
}
