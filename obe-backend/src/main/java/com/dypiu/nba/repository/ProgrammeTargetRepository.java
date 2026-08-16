package com.dypiu.nba.repository;

import com.dypiu.nba.entity.OutcomeType;
import com.dypiu.nba.entity.ProgrammeTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeTargetRepository extends JpaRepository<ProgrammeTarget, String> {
    List<ProgrammeTarget> findByBatchId(String batchId);
    List<ProgrammeTarget> findByBatchIdIn(Collection<String> batchIds);
    List<ProgrammeTarget> findByBatchIdAndOutcomeType(String batchId, OutcomeType outcomeType);
    Optional<ProgrammeTarget> findByBatchIdAndOutcomeCode(String batchId, String outcomeCode);
    Optional<ProgrammeTarget> findByBatchIdAndOutcomeTypeAndOutcomeCode(String batchId, OutcomeType outcomeType, String outcomeCode);
    void deleteByBatchId(String batchId);
}
