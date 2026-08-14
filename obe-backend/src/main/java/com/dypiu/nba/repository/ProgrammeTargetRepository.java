package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeTargetRepository extends JpaRepository<ProgrammeTarget, String> {
    List<ProgrammeTarget> findByProgrammeId(String programmeId);
    Optional<ProgrammeTarget> findByProgrammeIdAndOutcomeCode(String programmeId, String outcomeCode);
    void deleteByProgrammeId(String programmeId);
}
