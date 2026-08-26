package com.dypiu.nba.security;

import com.dypiu.nba.entity.Department;
import com.dypiu.nba.entity.MasterProgramme;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.DepartmentRepository;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates optional client filters against the authenticated user's persisted
 * scope.  A query parameter may narrow a result set, never broaden it.
 */
@Service
@RequiredArgsConstructor
public class RequestScopeAuthorizer {

    private final CurrentUserScopeService currentUserScopeService;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;

    public void assertRequestedSchool(String schoolId) {
        if (blank(schoolId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        if ((scope.isDirector() || scope.isHod() || scope.isProgrammeCoordinator())
                && !scope.getRequiredSchoolId().equals(schoolId)) {
            forbidden("school", schoolId);
        }
    }

    public void assertRequestedDepartment(String departmentId) {
        if (blank(departmentId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
        assertRequestedSchool(department.getSchoolId());
        if ((scope.isHod() || scope.isProgrammeCoordinator())
                && !scope.getRequiredDepartmentId().equals(departmentId)) {
            forbidden("department", departmentId);
        }
    }

    public void assertRequestedProgramme(String masterProgrammeId) {
        if (blank(masterProgrammeId)) return;
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (global(scope)) return;
        MasterProgramme programme = masterProgrammeRepository.findByIdAndDeletedAtIsNull(masterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + masterProgrammeId));
        assertRequestedDepartment(programme.getDepartmentId());
        if (scope.isProgrammeCoordinator() && !scope.getRequiredMasterProgrammeId().equals(masterProgrammeId)) {
            forbidden("programme", masterProgrammeId);
        }
    }

    private boolean global(CurrentUserScope scope) {
        return scope.isAdmin() || scope.isIqac();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void forbidden(String resource, String id) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Access denied: requested " + resource + " is outside your assigned scope (" + id + ").");
    }
}
