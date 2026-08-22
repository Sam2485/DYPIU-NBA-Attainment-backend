package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeAtr;
import com.dypiu.nba.entity.ProgrammeAtrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeAtrRepository extends JpaRepository<ProgrammeAtr, String> {
    Optional<ProgrammeAtr> findByProgrammeBatchId(String programmeBatchId);
    List<ProgrammeAtr> findByProgrammeBatchIdIn(Collection<String> programmeBatchIds);
    List<ProgrammeAtr> findByProgrammeBatchIdAndStatus(String programmeBatchId, ProgrammeAtrStatus status);
}
