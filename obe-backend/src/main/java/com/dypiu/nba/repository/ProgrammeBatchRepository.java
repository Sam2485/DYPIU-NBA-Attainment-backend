package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeBatchRepository extends JpaRepository<ProgrammeBatch, String> {

    Optional<ProgrammeBatch> findByIdAndDeletedAtIsNull(String id);

    List<ProgrammeBatch> findByDeletedAtIsNull();

    List<ProgrammeBatch> findByDeletedAtIsNullAndStatus(String status);

    List<ProgrammeBatch> findByMasterProgrammeIdAndDeletedAtIsNull(String masterProgrammeId);

    List<ProgrammeBatch> findByMasterProgrammeIdInAndDeletedAtIsNull(Collection<String> masterProgrammeIds);

    List<ProgrammeBatch> findByMasterProgrammeIdAndDeletedAtIsNullOrderByStartYearDesc(String masterProgrammeId);

    Optional<ProgrammeBatch> findByMasterProgrammeIdAndStartYearAndDeletedAtIsNull(String masterProgrammeId, Integer startYear);

    List<ProgrammeBatch> findByCoordinatorIdAndDeletedAtIsNull(Long coordinatorId);

    List<ProgrammeBatch> findByCoordinatorEmailIgnoreCaseAndDeletedAtIsNull(String coordinatorEmail);

    List<ProgrammeBatch> findByMasterProgrammeIdAndCoordinatorEmailIgnoreCaseAndDeletedAtIsNull(String masterProgrammeId, String coordinatorEmail);

    List<ProgrammeBatch> findByMasterProgrammeIdAndStatusAndDeletedAtIsNull(String masterProgrammeId, String status);

    @Query("""
        SELECT b FROM ProgrammeBatch b
        LEFT JOIN MasterProgramme mp ON b.masterProgrammeId = mp.id
        WHERE b.deletedAt IS NULL
          AND (mp.deletedAt IS NULL OR mp.id IS NULL)
          AND (:status IS NULL OR b.status IS NULL OR UPPER(b.status) = UPPER(:status))
          AND (:masterProgrammeId IS NULL OR b.masterProgrammeId = :masterProgrammeId)
          AND (:departmentId IS NULL OR mp.departmentId = :departmentId)
          AND (:coordinatorEmail IS NULL 
               OR (b.coordinatorEmail IS NOT NULL AND LOWER(TRIM(b.coordinatorEmail)) = LOWER(TRIM(:coordinatorEmail)))
               OR (b.coordinatorEmail IS NULL AND :masterProgrammeId IS NOT NULL AND b.masterProgrammeId = :masterProgrammeId)
              )
        ORDER BY b.startYear DESC
    """)
    List<ProgrammeBatch> findBatchesFiltered(
        @Param("masterProgrammeId") String masterProgrammeId,
        @Param("departmentId") String departmentId,
        @Param("coordinatorEmail") String coordinatorEmail,
        @Param("status") String status
    );

    @Query("""
        SELECT b FROM ProgrammeBatch b
        LEFT JOIN MasterProgramme mp ON b.masterProgrammeId = mp.id
        WHERE b.deletedAt IS NULL
          AND (mp.deletedAt IS NULL OR mp.id IS NULL)
          AND (:status IS NULL OR b.status IS NULL OR UPPER(b.status) = UPPER(:status))
          AND (:masterProgrammeId IS NULL OR b.masterProgrammeId = :masterProgrammeId)
          AND (mp.departmentId IN :departmentIds)
          AND (:coordinatorEmail IS NULL 
               OR (b.coordinatorEmail IS NOT NULL AND LOWER(TRIM(b.coordinatorEmail)) = LOWER(TRIM(:coordinatorEmail)))
               OR (b.coordinatorEmail IS NULL AND :masterProgrammeId IS NOT NULL AND b.masterProgrammeId = :masterProgrammeId)
              )
        ORDER BY b.startYear DESC
    """)
    List<ProgrammeBatch> findBatchesFilteredByDepartmentIds(
        @Param("masterProgrammeId") String masterProgrammeId,
        @Param("departmentIds") Collection<String> departmentIds,
        @Param("coordinatorEmail") String coordinatorEmail,
        @Param("status") String status
    );

    // Backward compatible queries for non-soft-deleted repository callers
    default List<ProgrammeBatch> findByMasterProgrammeId(String masterProgrammeId) {
        return findByMasterProgrammeIdAndDeletedAtIsNull(masterProgrammeId);
    }

    default List<ProgrammeBatch> findByMasterProgrammeIdIn(Collection<String> masterProgrammeIds) {
        return findByMasterProgrammeIdInAndDeletedAtIsNull(masterProgrammeIds);
    }

    default List<ProgrammeBatch> findByMasterProgrammeIdOrderByStartYearDesc(String masterProgrammeId) {
        return findByMasterProgrammeIdAndDeletedAtIsNullOrderByStartYearDesc(masterProgrammeId);
    }

    default Optional<ProgrammeBatch> findByMasterProgrammeIdAndStartYear(String masterProgrammeId, Integer startYear) {
        return findByMasterProgrammeIdAndStartYearAndDeletedAtIsNull(masterProgrammeId, startYear);
    }

    default List<ProgrammeBatch> findByCoordinatorId(Long coordinatorId) {
        return findByCoordinatorIdAndDeletedAtIsNull(coordinatorId);
    }

    default List<ProgrammeBatch> findByCoordinatorEmailIgnoreCase(String coordinatorEmail) {
        return findByCoordinatorEmailIgnoreCaseAndDeletedAtIsNull(coordinatorEmail);
    }

    default List<ProgrammeBatch> findByMasterProgrammeIdAndStatus(String masterProgrammeId, String status) {
        return findByMasterProgrammeIdAndStatusAndDeletedAtIsNull(masterProgrammeId, status);
    }
}
