package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeCoordinatorSetupProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgrammeCoordinatorSetupProgressRepository extends JpaRepository<ProgrammeCoordinatorSetupProgress, String> {
    Optional<ProgrammeCoordinatorSetupProgress> findByProgrammeBatchId(String programmeBatchId);
    Optional<ProgrammeCoordinatorSetupProgress> findByCoordinatorEmailIgnoreCase(String coordinatorEmail);
}
