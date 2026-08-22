package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.entity.PeoOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeoOutcomeRepository extends JpaRepository<PeoOutcome, String> {
    List<PeoOutcome> findByProgrammeBatchId(String programmeBatchId);
    List<PeoOutcome> findByProgrammeBatchIdOrderByCodeAsc(String programmeBatchId);
    List<PeoOutcome> findByProgrammeBatchIdAndStatus(String programmeBatchId, ApprovalStatus status);
    Optional<PeoOutcome> findByProgrammeBatchIdAndCode(String programmeBatchId, String code);
    void deleteByProgrammeBatchId(String programmeBatchId);
}
