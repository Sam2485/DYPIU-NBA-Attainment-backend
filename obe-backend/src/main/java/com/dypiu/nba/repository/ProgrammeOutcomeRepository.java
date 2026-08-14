package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgrammeOutcomeRepository extends JpaRepository<ProgrammeOutcome, String> {
    List<ProgrammeOutcome> findByProgrammeId(String programmeId);
    List<ProgrammeOutcome> findByProgrammeIdOrderByCodeAsc(String programmeId);
    List<ProgrammeOutcome> findByProgrammeIdAndAcademicYear(String programmeId, String academicYear);
}
