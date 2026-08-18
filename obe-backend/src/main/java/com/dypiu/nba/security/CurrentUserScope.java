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
 * - ADMIN: Global access (schoolId may be optional/null)
 * - IQAC: Institution-wide quality assurance (schoolId may be optional/null)
 * - DIRECTOR: Scoped to schoolId
 * - HOD: Scoped to schoolId + departmentId
 * - PROGRAMME_COORDINATOR: Scoped to schoolId + departmentId + programmeId
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
    private final String programmeId;

    public boolean hasSchoolScope() {
        return schoolId != null && !schoolId.isBlank();
    }

    public boolean hasDepartmentScope() {
        return departmentId != null && !departmentId.isBlank();
    }

    public boolean hasProgrammeScope() {
        return programmeId != null && !programmeId.isBlank();
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
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
     * Retrieves the mandatory programmeId for the authenticated user.
     * Throws a 403 Forbidden ResponseStatusException if programmeId is missing or empty.
     * Never falls back to a default or arbitrary programme.
     */
    public String getRequiredProgrammeId() {
        if (!hasProgrammeScope()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Authenticated user '" + (email != null ? email : username) + "' has no assigned programme scope.");
        }
        return programmeId;
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
                Objects.equals(programmeId, that.programmeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, email, role, schoolId, departmentId, programmeId);
    }
}
