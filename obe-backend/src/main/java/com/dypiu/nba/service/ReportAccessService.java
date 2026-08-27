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
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;

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
            return userRepository.findAll().stream().findFirst().orElse(null);
        }

        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }

    @Transactional(readOnly = true)
    public void validateProgrammeAccess(User user, String masterProgrammeId) {
        System.out.println("[ReportAccessService] validateProgrammeAccess called | user: " + (user != null ? user.getEmail() : "null") + " | masterProgrammeId: " + masterProgrammeId);
        if (user == null || masterProgrammeId == null) return;
        if (user.getRole() == UserRole.IQAC) return;

        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme not found: " + masterProgrammeId));

        if (user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            if (user.getMasterProgrammeId() != null && !user.getMasterProgrammeId().equals(masterProgrammeId)) {
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
    public void validateBatchAccess(User user, String programmeBatchId) {
        System.out.println("[ReportAccessService] validateBatchAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeBatchId: " + programmeBatchId);
        if (user == null || programmeBatchId == null) return;
        if (user.getRole() == UserRole.IQAC) return;

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + programmeBatchId));

        validateProgrammeAccess(user, batch.getMasterProgrammeId());
    }

    @Transactional(readOnly = true)
    public void validateCourseOfferingAccess(User user, String programmeBatchCourseId) {
        System.out.println("[ReportAccessService] validateCourseOfferingAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeBatchCourseId: " + programmeBatchCourseId);
        if (user == null || programmeBatchCourseId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC) return;

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + programmeBatchCourseId));

        if (user.getRole() == UserRole.FACULTY) {
            boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), user.getId()));
            if (!isCoordinator) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
            return;
        }

        validateBatchAccess(user, offering.getProgrammeBatchId());
    }

    @Transactional(readOnly = true)
    public void validateCourseCoordinatorAccess(User user, String programmeBatchCourseId) {
        System.out.println("[ReportAccessService] validateCourseCoordinatorAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeBatchCourseId: " + programmeBatchCourseId);
        if (user == null || programmeBatchCourseId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC || user.getRole() == UserRole.DIRECTOR || user.getRole() == UserRole.HOD || user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            validateCourseOfferingAccess(user, programmeBatchCourseId);
            return;
        }

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + programmeBatchCourseId));

        boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), user.getId()));
        if (!isCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned Course Coordinator can perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public void validateCourseAccess(User user, String masterCourseId) {
        System.out.println("[ReportAccessService] validateCourseAccess called | user: " + (user != null ? user.getEmail() : "null") + " | masterCourseId: " + masterCourseId);
        if (user == null || masterCourseId == null) return;
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.IQAC) return;

        MasterCourse course = masterCourseRepository.findById(masterCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + masterCourseId));

        if (user.getRole() == UserRole.FACULTY) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(masterCourseId);
            boolean hasAssignedOffering = offerings.stream().anyMatch(o -> 
                (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), user.getId()))
            );
            if (!hasAssignedOffering) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to any offering of this Course.");
            }
            return;
        }

        validateProgrammeAccess(user, course.getMasterProgrammeId());
    }

    @Transactional(readOnly = true)
    public void validateCourseAtrAccess(User user, String programmeBatchCourseId) {
        System.out.println("[ReportAccessService] validateCourseAtrAccess called | user: " + (user != null ? user.getEmail() : "null") + " | programmeBatchCourseId: " + programmeBatchCourseId);
        validateCourseOfferingAccess(user, programmeBatchCourseId);
    }

    @Transactional(readOnly = true)
    public void validateProgrammeAtrAccess(User user, String masterProgrammeId, String programmeBatchId) {
        System.out.println("[ReportAccessService] validateProgrammeAtrAccess called | user: " + (user != null ? user.getEmail() : "null") + " | masterProgrammeId: " + masterProgrammeId + " | programmeBatchId: " + programmeBatchId);
        if (user == null) return;
        if (user.getRole() == UserRole.FACULTY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course Coordinators / Faculty do not have permission to view or edit Programme ATRs.");
        }
        if (masterProgrammeId != null) {
            validateProgrammeAccess(user, masterProgrammeId);
        }
        if (programmeBatchId != null) {
            validateBatchAccess(user, programmeBatchId);
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
        List<MasterProgramme> allowedProgrammes = new ArrayList<>();
        List<ProgrammeBatch> allowedBatches = new ArrayList<>();
        List<ProgrammeBatchCourse> allowedOfferings = new ArrayList<>();

        if (user.getRole() == UserRole.IQAC) {
            allowedProgrammes = masterProgrammeRepository.findAll();
            allowedBatches = programmeBatchRepository.findAll();
            allowedOfferings = programmeBatchCourseRepository.findAll();
        } else if (user.getRole() == UserRole.DIRECTOR) {
            String schoolId = user.getSchoolId();
            List<Department> depts = schoolId != null ? departmentRepository.findBySchoolId(schoolId) : departmentRepository.findAll();
            Set<String> deptIds = depts.stream().map(Department::getId).collect(Collectors.toSet());
            allowedProgrammes = masterProgrammeRepository.findAll().stream().filter(p -> deptIds.contains(p.getDepartmentId())).collect(Collectors.toList());
            Set<String> progIds = allowedProgrammes.stream().map(MasterProgramme::getId).collect(Collectors.toSet());
            allowedBatches = programmeBatchRepository.findAll().stream().filter(b -> progIds.contains(b.getMasterProgrammeId())).collect(Collectors.toList());
            Set<String> programmeBatchIds = allowedBatches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            allowedOfferings = programmeBatchCourseRepository.findByProgrammeBatchIdIn(programmeBatchIds);
        } else if (user.getRole() == UserRole.HOD) {
            String deptId = user.getDepartmentId();
            allowedProgrammes = deptId != null ? masterProgrammeRepository.findByDepartmentId(deptId) : masterProgrammeRepository.findAll();
            Set<String> progIds = allowedProgrammes.stream().map(MasterProgramme::getId).collect(Collectors.toSet());
            allowedBatches = programmeBatchRepository.findAll().stream().filter(b -> progIds.contains(b.getMasterProgrammeId())).collect(Collectors.toList());
            Set<String> programmeBatchIds = allowedBatches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            allowedOfferings = programmeBatchCourseRepository.findByProgrammeBatchIdIn(programmeBatchIds);
        } else if (user.getRole() == UserRole.PROGRAMME_COORDINATOR) {
            String progId = user.getMasterProgrammeId();
            allowedProgrammes = progId != null ? masterProgrammeRepository.findById(progId).map(List::of).orElse(Collections.emptyList()) : masterProgrammeRepository.findAll();
            Set<String> progIds = allowedProgrammes.stream().map(MasterProgramme::getId).collect(Collectors.toSet());
            allowedBatches = programmeBatchRepository.findAll().stream().filter(b -> progIds.contains(b.getMasterProgrammeId())).collect(Collectors.toList());
            Set<String> programmeBatchIds = allowedBatches.stream().map(ProgrammeBatch::getId).collect(Collectors.toSet());
            allowedOfferings = programmeBatchCourseRepository.findByProgrammeBatchIdIn(programmeBatchIds);
        } else if (user.getRole() == UserRole.FACULTY) {
            List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findAll().stream()
                    .filter(o -> o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), user.getId()))
                    .collect(Collectors.toList());
            allowedOfferings = offerings;
            Set<String> programmeBatchIds = offerings.stream().map(ProgrammeBatchCourse::getProgrammeBatchId).collect(Collectors.toSet());
            allowedBatches = programmeBatchRepository.findAll().stream().filter(b -> programmeBatchIds.contains(b.getId())).collect(Collectors.toList());
            Set<String> progIds = allowedBatches.stream().map(ProgrammeBatch::getMasterProgrammeId).collect(Collectors.toSet());
            allowedProgrammes = masterProgrammeRepository.findAll().stream().filter(p -> progIds.contains(p.getId())).collect(Collectors.toList());
        }

        Map<String, MasterCourse> courseMap = masterCourseRepository.findAll().stream().collect(Collectors.toMap(MasterCourse::getId, c -> c, (a, b) -> a));

        List<ReportFiltersDto.Item> progItems = allowedProgrammes.stream()
                .map(p -> ReportFiltersDto.Item.builder().id(p.getId()).name(p.getName()).code(p.getCode()).build())
                .collect(Collectors.toList());

        List<ReportFiltersDto.BatchItem> batchItems = allowedBatches.stream()
                .map(b -> ReportFiltersDto.BatchItem.builder().id(b.getId()).masterProgrammeId(b.getMasterProgrammeId()).name(b.getName()).build())
                .collect(Collectors.toList());

        List<ReportFiltersDto.OfferingItem> offeringItems = allowedOfferings.stream()
                .map(o -> {
                    MasterCourse c = courseMap.get(o.getMasterCourseId());
                    return ReportFiltersDto.OfferingItem.builder()
                            .id(o.getId())
                            .masterCourseId(o.getMasterCourseId())
                            .programmeBatchId(o.getProgrammeBatchId())
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
