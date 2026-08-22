package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeBatchRepository extends JpaRepository<ProgrammeBatch, String> {
    List<ProgrammeBatch> findByMasterProgrammeId(String masterProgrammeId);
    List<ProgrammeBatch> findByMasterProgrammeIdIn(Collection<String> masterProgrammeIds);
    List<ProgrammeBatch> findByMasterProgrammeIdOrderByStartYearDesc(String masterProgrammeId);
    Optional<ProgrammeBatch> findByMasterProgrammeIdAndStartYear(String masterProgrammeId, Integer startYear);
    List<ProgrammeBatch> findByCoordinatorId(Long coordinatorId);
    List<ProgrammeBatch> findByCoordinatorEmailIgnoreCase(String coordinatorEmail);
    List<ProgrammeBatch> findByMasterProgrammeIdAndStatus(String masterProgrammeId, String status);
}
