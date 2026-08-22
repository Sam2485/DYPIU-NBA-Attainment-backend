package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.entity.ProgrammeOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeOutcomeRepository extends JpaRepository<ProgrammeOutcome, String> {
    List<ProgrammeOutcome> findByProgrammeBatchId(String programmeBatchId);
    List<ProgrammeOutcome> findByProgrammeBatchIdOrderByCodeAsc(String programmeBatchId);
    List<ProgrammeOutcome> findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status);
    Optional<ProgrammeOutcome> findByProgrammeBatchIdAndCode(String programmeBatchId, String code);
    void deleteByProgrammeBatchId(String programmeBatchId);
}
