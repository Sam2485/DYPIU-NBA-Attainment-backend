package com.dypiu.nba.repository;

import com.dypiu.nba.entity.UserOrganizationalAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationalAssignmentRepository extends JpaRepository<UserOrganizationalAssignment, Long> {

    List<UserOrganizationalAssignment> findByUserId(Long userId);

    List<UserOrganizationalAssignment> findByUserIdAndIsActiveTrue(Long userId);

    List<UserOrganizationalAssignment> findByUserIdInAndIsActiveTrue(Collection<Long> userIds);

    List<UserOrganizationalAssignment> findAllByIsActiveTrue();

    List<UserOrganizationalAssignment> findBySchoolIdAndIsActiveTrue(String schoolId);

    List<UserOrganizationalAssignment> findByDepartmentIdAndIsActiveTrue(String departmentId);

    List<UserOrganizationalAssignment> findByMasterProgrammeIdAndIsActiveTrue(String masterProgrammeId);

    @Query("SELECT a FROM UserOrganizationalAssignment a WHERE a.userId = :userId AND UPPER(a.role) = UPPER(:role) " +
            "AND ((:schoolId IS NULL AND a.schoolId IS NULL) OR a.schoolId = :schoolId) " +
            "AND ((:departmentId IS NULL AND a.departmentId IS NULL) OR a.departmentId = :departmentId) " +
            "AND ((:masterProgrammeId IS NULL AND a.masterProgrammeId IS NULL) OR a.masterProgrammeId = :masterProgrammeId)")
    List<UserOrganizationalAssignment> findMatchingAssignments(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("schoolId") String schoolId,
            @Param("departmentId") String departmentId,
            @Param("masterProgrammeId") String masterProgrammeId
    );

    void deleteByUserId(Long userId);
}
