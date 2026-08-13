package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeSpecificOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgrammeSpecificOutcomeRepository extends JpaRepository<ProgrammeSpecificOutcome, String> {
    List<ProgrammeSpecificOutcome> findByProgrammeId(String programmeId);
    List<ProgrammeSpecificOutcome> findByProgrammeIdAndAcademicYear(String programmeId, String academicYear);
}
