package com.dypiu.nba.security;

import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.entity.ProgrammeBatch;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.ProgrammeBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Validates optional client filters against the authenticated user's persisted scope.
 * A query parameter may narrow a result set, never broaden it.
 */
@Service
@RequiredArgsConstructor
public class RequestScopeAuthorizer {

    private final CurrentUserScopeService currentUserScopeService;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;

    public void assertRequestedSchool(String schoolId) {
        if (blank(schoolId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if ((scope.isDirector() || scope.isHod() || scope.isProgrammeCoordinator() || scope.isFaculty())
                && scope.hasSchoolScope()
                && !scope.getRequiredSchoolId().equalsIgnoreCase(schoolId.trim())) {
            forbidden("school", schoolId);
        }
    }

    public void assertRequestedDepartment(String departmentId) {
        if (blank(departmentId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        Department department = departmentRepository.findById(departmentId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
        assertRequestedSchool(department.getSchoolId());
        if (scope.isHod()) {
            if (scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<Department> hodDepts = departmentRepository.findByHodEmailIgnoreCase(scope.getEmail().trim());
                if (hodDepts != null && !hodDepts.isEmpty()) {
                    boolean match = hodDepts.stream().anyMatch(d -> departmentId.trim().equalsIgnoreCase(d.getId()));
                    if (!match) {
                        forbidden("department", departmentId);
                    }
                    return;
                }
            }
            if (!scope.getRequiredDepartmentId().equalsIgnoreCase(departmentId.trim())) {
                forbidden("department", departmentId);
            }
        }
        if (scope.isProgrammeCoordinator() || scope.isFaculty()) {
            if (scope.hasDepartmentScope() && !scope.getDepartmentId().equalsIgnoreCase(departmentId.trim())) {
                forbidden("department", departmentId);
            }
        }
    }

    public void assertRequestedProgramme(String masterProgrammeId) {
        if (blank(masterProgrammeId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        MasterProgramme programme = masterProgrammeRepository.findByIdAndDeletedAtIsNull(masterProgrammeId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("MasterProgramme not found: " + masterProgrammeId));
        assertRequestedDepartment(programme.getDepartmentId());
        if (scope.isProgrammeCoordinator()) {
            boolean matchesDirect = scope.getMasterProgrammeId() != null && scope.getMasterProgrammeId().equalsIgnoreCase(masterProgrammeId.trim());
            boolean matchesBatch = false;
            if (!matchesDirect && scope.getEmail() != null && !scope.getEmail().isBlank()) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByCoordinatorEmailIgnoreCaseAndDeletedAtIsNull(scope.getEmail().trim());
                matchesBatch = batches.stream().anyMatch(b -> masterProgrammeId.trim().equalsIgnoreCase(b.getMasterProgrammeId()));
            }
            if (!matchesDirect && !matchesBatch) {
                forbidden("programme", masterProgrammeId);
            }
        }
    }

    public void assertRequestedCoordinatorEmail(String coordinatorEmail) {
        if (blank(coordinatorEmail)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if (scope.isProgrammeCoordinator()) {
            if (scope.getEmail() == null || !scope.getEmail().trim().equalsIgnoreCase(coordinatorEmail.trim())) {
                forbidden("coordinator email", coordinatorEmail);
            }
        } else if (scope.isFaculty() || scope.isHod() || scope.isDirector()) {
            forbidden("coordinator email", coordinatorEmail);
        }
    }

    public void assertRequestedCourseCoordinatorEmail(String courseCoordinatorEmail) {
        if (blank(courseCoordinatorEmail)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if (scope.isFaculty()) {
            if (scope.getEmail() == null || !scope.getEmail().trim().equalsIgnoreCase(courseCoordinatorEmail.trim())) {
                forbidden("course coordinator email", courseCoordinatorEmail);
            }
        } else if (scope.isProgrammeCoordinator() || scope.isHod() || scope.isDirector()) {
            forbidden("course coordinator email", courseCoordinatorEmail);
        }
    }

    public void assertRequestedHodEmail(String hodEmail) {
        if (blank(hodEmail)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if (scope.isHod()) {
            if (scope.getEmail() == null || !scope.getEmail().trim().equalsIgnoreCase(hodEmail.trim())) {
                forbidden("HOD email", hodEmail);
            }
        } else {
            forbidden("HOD email", hodEmail);
        }
    }

    public void assertRequestedDirectorEmail(String directorEmail) {
        if (blank(directorEmail)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if (scope.isDirector()) {
            if (scope.getEmail() == null || !scope.getEmail().trim().equalsIgnoreCase(directorEmail.trim())) {
                forbidden("Director email", directorEmail);
            }
        } else {
            forbidden("Director email", directorEmail);
        }
    }

    public void assertRequestedUserEmail(String userEmail, String role) {
        if (blank(userEmail)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if (scope.getEmail() == null || !scope.getEmail().trim().equalsIgnoreCase(userEmail.trim())) {
            forbidden("user email", userEmail);
        }
        if (!blank(role)) {
            if (scope.getRole() == null || !scope.getRole().name().equalsIgnoreCase(role.trim())) {
                forbidden("role", role);
            }
        }
    }

    private boolean global(CurrentUserScope scope) {
        return scope == null || scope.isAdmin() || scope.isIqac();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void forbidden(String resource, String id) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: requested " + resource + " is outside your assigned scope (" + id + ").");
    }
}
