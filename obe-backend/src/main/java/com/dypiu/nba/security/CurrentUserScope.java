package com.dypiu.nba.security;

import com.dypiu.nba.entity.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable value object representing the authenticated user's identity and organizational scope.
 *
 * Scope Hierarchy:
 * - IQAC: Institution-wide administrative & quality assurance authority (can manage users, schools, report headers/logos, and access all reports across all schools)
 * - DIRECTOR: Scoped to schoolId
 * - HOD: Scoped to schoolId + departmentId
 * - PROGRAMME_COORDINATOR: Scoped to schoolId + departmentId + masterProgrammeId
 * - FACULTY: Scoped to assigned courses / offerings within schoolId + departmentId
 */
@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CurrentUserScope implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String email;
    private final String name;
    private final UserRole role;
    private final String schoolId;
    private final String departmentId;
    private final String masterProgrammeId;

    public boolean hasSchoolScope() {
        return schoolId != null && !schoolId.isBlank();
    }

    public boolean hasDepartmentScope() {
        return departmentId != null && !departmentId.isBlank();
    }

    public boolean hasProgrammeScope() {
        return masterProgrammeId != null && !masterProgrammeId.isBlank();
    }

    public boolean isDirector() {
        return role == UserRole.DIRECTOR;
    }

    public boolean isHod() {
        return role == UserRole.HOD;
    }

    public boolean isProgrammeCoordinator() {
        return role == UserRole.PROGRAMME_COORDINATOR;
    }

    public boolean isFaculty() {
        return role == UserRole.FACULTY;
    }

    public boolean isIqac() {
        return role == UserRole.IQAC;
    }

    /**
     * Retrieves the mandatory schoolId for the authenticated user.
     * Throws a 403 Forbidden ResponseStatusException if schoolId is missing or empty.
     * Never falls back to a default or arbitrary school.
     */
    public String getRequiredSchoolId() {
        if (!hasSchoolScope()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Authenticated user '" + (email != null ? email : username) + "' has no assigned school scope.");
        }
        return schoolId;
    }

    /**
     * Retrieves the mandatory departmentId for the authenticated user.
     * Throws a 403 Forbidden ResponseStatusException if departmentId is missing or empty.
     * Never falls back to a default or arbitrary department.
     */
    public String getRequiredDepartmentId() {
        if (!hasDepartmentScope()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Authenticated user '" + (email != null ? email : username) + "' has no assigned department scope.");
        }
        return departmentId;
    }

    /**
     * Retrieves the mandatory masterProgrammeId for the authenticated user.
     * Throws a 403 Forbidden ResponseStatusException if masterProgrammeId is missing or empty.
     * Never falls back to a default or arbitrary programme.
     */
    public String getRequiredMasterProgrammeId() {
        if (!hasProgrammeScope()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Authenticated user '" + (email != null ? email : username) + "' has no assigned programme scope.");
        }
        return masterProgrammeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentUserScope that = (CurrentUserScope) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(email, that.email) &&
                role == that.role &&
                Objects.equals(schoolId, that.schoolId) &&
                Objects.equals(departmentId, that.departmentId) &&
                Objects.equals(masterProgrammeId, that.masterProgrammeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, email, role, schoolId, departmentId, masterProgrammeId);
    }
}
