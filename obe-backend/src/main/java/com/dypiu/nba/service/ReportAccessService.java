package com.dypiu.nba.service;

import com.dypiu.nba.dto.ReportFiltersDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportAccessService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    @Transactional(readOnly = true)
    public User getAuthenticatedUser(Principal principal) {
        System.out.println("[ReportAccessService] getAuthenticatedUser called | principal: " + (principal != null ? principal.getName() : "null"));
        String usernameOrEmail = null;
        if (principal != null) {
            usernameOrEmail = principal.getName();
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                usernameOrEmail = auth.getName();
            }
        }

        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            // Default to first active user if in unauthenticated/testing context
            return userRepository.findAll().stream().findFirst().orElse(null);
        }

        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }

    @Transactional(readOnly = true)
    public void validateProgrammeAccess(User user, String programmeId) {
        System.out.println("[ReportAccessService] validateProgrammeAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeId: " + programmeId);
        if (user == null || programmeId == null) return;
        if (user.getRole() == UserRole.IQAC) return;

        Programme prog = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme not found: " + programmeId));

        if (user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            if (user.getProgrammeId() != null && !user.getProgrammeId().equals(programmeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your assigned programme scope.");
            }
            return;
        }

        Department dept = departmentRepository.findById(prog.getDepartmentId()).orElse(null);
        if (user.getRole() == UserRole.HOD) {
            if (user.getDepartmentId() != null && dept != null && !user.getDepartmentId().equals(dept.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your department scope.");
            }
            return;
        }

        if (user.getRole() == UserRole.DIRECTOR) {
            if (user.getSchoolId() != null && dept != null && !user.getSchoolId().equals(dept.getSchoolId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme is outside your school scope.");
            }
        }
    }

    @Transactional(readOnly = true)
    public void validateBatchAccess(User user, String batchId) {
        System.out.println("[ReportAccessService] validateBatchAccess called | user: " + (user != null ? user.getEmail() : "null") + " | batchId: " + batchId);
        if (user == null || batchId == null) return;
        if (user.getRole() == UserRole.IQAC) return;

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId));

        validateProgrammeAccess(user, batch.getProgrammeId());
    }

    @Transactional(readOnly = true)
    public void validateCourseOfferingAccess(User user, String courseOfferingId) {
        System.out.println("[ReportAccessService] validateCourseOfferingAccess called | user: " + (user != null ? user.getEmail() : "null") + " | courseOfferingId: " + courseOfferingId);
        if (user == null || courseOfferingId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC) return;

        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + courseOfferingId));

        if (user.getRole() == UserRole.FACULTY) {
            boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), user.getId()));
            if (!isCoordinator) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
            return;
        }

        validateBatchAccess(user, offering.getBatchId());
    }

    @Transactional(readOnly = true)
    public void validateCourseCoordinatorAccess(User user, String courseOfferingId) {
        System.out.println("[ReportAccessService] validateCourseCoordinatorAccess called | user: " + (user != null ? user.getEmail() : "null") + " | courseOfferingId: " + courseOfferingId);
        if (user == null || courseOfferingId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC || user.getRole() == UserRole.DIRECTOR || user.getRole() == UserRole.HOD || user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            validateCourseOfferingAccess(user, courseOfferingId);
            return;
        }

        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + courseOfferingId));

        boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), user.getId()));
        if (!isCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned Course Coordinator can perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public void validateCourseAccess(User user, String courseId) {
        System.out.println("[ReportAccessService] validateCourseAccess called | user: " + (user != null ? user.getEmail() : "null") + " | courseId: " + courseId);
        if (user == null || courseId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC) return;

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));

        if (user.getRole() == UserRole.FACULTY) {
            List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
            boolean hasAssignedOffering = offerings.stream().anyMatch(o -> 
                (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), user.getId()))
            );
            if (!hasAssignedOffering) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to any offering of this Course.");
            }
            return;
        }

        validateProgrammeAccess(user, course.getProgrammeId());
    }

    @Transactional(readOnly = true)
    public void validateCourseAtrAccess(User user, String courseOfferingId) {
        System.out.println("[ReportAccessService] validateCourseAtrAccess called | user: " + (user != null ? user.getEmail() : "null") + " | courseOfferingId: " + courseOfferingId);
        validateCourseOfferingAccess(user, courseOfferingId);
    }

    @Transactional(readOnly = true)
    public void validateProgrammeAtrAccess(User user, String programmeId, String batchId) {
        System.out.println("[ReportAccessService] validateProgrammeAtrAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeId: " + programmeId + " | batchId: " + batchId);
        if (user == null) return;
        if (user.getRole() == UserRole.FACULTY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course Coordinators / Faculty do not have permission to view or edit Programme ATRs.");
        }
        if (programmeId != null) {
            validateProgrammeAccess(user, programmeId);
        }
        if (batchId != null) {
            validateBatchAccess(user, batchId);
        }
    }

    @Transactional(readOnly = true)
    public ReportFiltersDto getReportFilters(User user) {
        System.out.println("[ReportAccessService] getReportFilters called | user: " + (user != null ? user.getEmail() : "null"));
        if (user == null) {
            return ReportFiltersDto.builder()
                    .role("GUEST")
                    .programmes(Collections.emptyList())
                    .batches(Collections.emptyList())
                    .courseOfferings(Collections.emptyList())
                    .build();
        }

        String roleStr = user.getRole().name();
        List<Programme> allowedProgrammes = new ArrayList<>();
        List<Batch> allowedBatches = new ArrayList<>();
        List<CourseOffering> allowedOfferings = new ArrayList<>();

        if (user.getRole() == UserRole.IQAC) {
            allowedProgrammes = programmeRepository.findAll();
            allowedBatches = batchRepository.findAll();
            allowedOfferings = courseOfferingRepository.findAll();
        } else if (user.getRole() == UserRole.DIRECTOR) {
            String schoolId = user.getSchoolId();
            List<Department> depts = schoolId != null ? departmentRepository.findBySchoolId(schoolId) : departmentRepository.findAll();
            Set<String> deptIds = depts.stream().map(Department::getId).collect(Collectors.toSet());
            allowedProgrammes = programmeRepository.findAll().stream().filter(p -> deptIds.contains(p.getDepartmentId())).collect(Collectors.toList());
            Set<String> progIds = allowedProgrammes.stream().map(Programme::getId).collect(Collectors.toSet());
            allowedBatches = batchRepository.findAll().stream().filter(b -> progIds.contains(b.getProgrammeId())).collect(Collectors.toList());
            Set<String> batchIds = allowedBatches.stream().map(Batch::getId).collect(Collectors.toSet());
            allowedOfferings = courseOfferingRepository.findByBatchIdIn(batchIds);
        } else if (user.getRole() == UserRole.HOD) {
            String deptId = user.getDepartmentId();
            allowedProgrammes = deptId != null ? programmeRepository.findByDepartmentId(deptId) : programmeRepository.findAll();
            Set<String> progIds = allowedProgrammes.stream().map(Programme::getId).collect(Collectors.toSet());
            allowedBatches = batchRepository.findAll().stream().filter(b -> progIds.contains(b.getProgrammeId())).collect(Collectors.toList());
            Set<String> batchIds = allowedBatches.stream().map(Batch::getId).collect(Collectors.toSet());
            allowedOfferings = courseOfferingRepository.findByBatchIdIn(batchIds);
        } else if (user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            String progId = user.getProgrammeId();
            allowedProgrammes = progId != null ? programmeRepository.findById(progId).map(List::of).orElse(Collections.emptyList()) : programmeRepository.findAll();
            Set<String> progIds = allowedProgrammes.stream().map(Programme::getId).collect(Collectors.toSet());
            allowedBatches = batchRepository.findAll().stream().filter(b -> progIds.contains(b.getProgrammeId())).collect(Collectors.toList());
            Set<String> batchIds = allowedBatches.stream().map(Batch::getId).collect(Collectors.toSet());
            allowedOfferings = courseOfferingRepository.findByBatchIdIn(batchIds);
        } else if (user.getRole() == UserRole.FACULTY) {
            // Course Coordinator
            List<CourseOffering> offerings = courseOfferingRepository.findAll().stream()
                    .filter(o -> o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), user.getId()))
                    .collect(Collectors.toList());
            allowedOfferings = offerings;
            Set<String> batchIds = offerings.stream().map(CourseOffering::getBatchId).collect(Collectors.toSet());
            allowedBatches = batchRepository.findAll().stream().filter(b -> batchIds.contains(b.getId())).collect(Collectors.toList());
            Set<String> progIds = allowedBatches.stream().map(Batch::getProgrammeId).collect(Collectors.toSet());
            allowedProgrammes = programmeRepository.findAll().stream().filter(p -> progIds.contains(p.getId())).collect(Collectors.toList());
        }

        Map<String, Course> courseMap = courseRepository.findAll().stream().collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        List<ReportFiltersDto.Item> progItems = allowedProgrammes.stream()
                .map(p -> ReportFiltersDto.Item.builder().id(p.getId()).name(p.getName()).code(p.getCode()).build())
                .collect(Collectors.toList());

        List<ReportFiltersDto.BatchItem> batchItems = allowedBatches.stream()
                .map(b -> ReportFiltersDto.BatchItem.builder().id(b.getId()).programmeId(b.getProgrammeId()).name(b.getName()).build())
                .collect(Collectors.toList());

        List<ReportFiltersDto.OfferingItem> offeringItems = allowedOfferings.stream()
                .map(o -> {
                    Course c = courseMap.get(o.getCourseId());
                    return ReportFiltersDto.OfferingItem.builder()
                            .id(o.getId())
                            .courseId(o.getCourseId())
                            .batchId(o.getBatchId())
                            .courseCode(c != null ? c.getCode() : "N/A")
                            .courseName(c != null ? c.getName() : "N/A")
                            .semester(o.getSemester())
                            .build();
                })
                .collect(Collectors.toList());

        return ReportFiltersDto.builder()
                .role(roleStr)
                .programmes(progItems)
                .batches(batchItems)
                .courseOfferings(offeringItems)
                .build();
    }
}
