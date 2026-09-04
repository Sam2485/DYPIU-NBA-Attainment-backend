package com.dypiu.nba.reports.repository;

import com.dypiu.nba.reports.model.ReportEntity;
import com.dypiu.nba.reports.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, String> {
    List<ReportEntity> findAllByOrderByGeneratedAtDesc();
    List<ReportEntity> findByInstitutionIdOrderByGeneratedAtDesc(String institutionId);
    List<ReportEntity> findByProgrammeBatchIdOrderByGeneratedAtDesc(String programmeBatchId);
    List<ReportEntity> findByProgrammeBatchCourseIdOrderByGeneratedAtDesc(String programmeBatchCourseId);
    List<ReportEntity> findByReportTypeAndProgrammeBatchIdOrderByGeneratedAtDesc(ReportType reportType, String programmeBatchId);
    List<ReportEntity> findByReportTypeAndProgrammeBatchCourseIdOrderByGeneratedAtDesc(ReportType reportType, String programmeBatchCourseId);
    Optional<ReportEntity> findFirstByReportTypeAndProgrammeBatchIdOrderByGeneratedAtDesc(ReportType reportType, String programmeBatchId);
    Optional<ReportEntity> findFirstByReportTypeAndProgrammeBatchCourseIdOrderByGeneratedAtDesc(ReportType reportType, String programmeBatchCourseId);
}
