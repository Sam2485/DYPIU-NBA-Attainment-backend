package com.dypiu.nba.repository;

import com.dypiu.nba.entity.PeoOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeoOutcomeRepository extends JpaRepository<PeoOutcome, String> {
    List<PeoOutcome> findByProgrammeId(String programmeId);
    List<PeoOutcome> findByProgrammeIdOrderByCodeAsc(String programmeId);
}
