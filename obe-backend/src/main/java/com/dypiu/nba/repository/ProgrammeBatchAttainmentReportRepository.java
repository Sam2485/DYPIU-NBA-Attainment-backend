package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeBatchAttainmentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeBatchAttainmentReportRepository extends JpaRepository<ProgrammeBatchAttainmentReport, String> {
    Optional<ProgrammeBatchAttainmentReport> findByProgrammeBatchId(String programmeBatchId);
    List<ProgrammeBatchAttainmentReport> findByProgrammeBatchIdIn(List<String> programmeBatchIds);
}
