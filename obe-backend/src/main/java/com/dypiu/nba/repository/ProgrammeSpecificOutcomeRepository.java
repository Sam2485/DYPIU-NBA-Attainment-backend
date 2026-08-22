package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.entity.ProgrammeSpecificOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeSpecificOutcomeRepository extends JpaRepository<ProgrammeSpecificOutcome, String> {
    List<ProgrammeSpecificOutcome> findByProgrammeBatchId(String programmeBatchId);
    List<ProgrammeSpecificOutcome> findByProgrammeBatchIdOrderByCodeAsc(String programmeBatchId);
    List<ProgrammeSpecificOutcome> findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status);
    Optional<ProgrammeSpecificOutcome> findByProgrammeBatchIdAndCode(String programmeBatchId, String code);
    void deleteByProgrammeBatchId(String programmeBatchId);
}
