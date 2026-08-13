package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeAtr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProgrammeAtrRepository extends JpaRepository<ProgrammeAtr, String> {
    Optional<ProgrammeAtr> findByProgrammeId(String programmeId);
    Optional<ProgrammeAtr> findByProgrammeIdAndBatchId(String programmeId, String batchId);
}
