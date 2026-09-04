package com.dypiu.nba.reports.repository;

import com.dypiu.nba.reports.model.ReportTemplateEntity;
import com.dypiu.nba.reports.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplateEntity, String> {
    Optional<ReportTemplateEntity> findFirstByIsDefaultTrue();
    Optional<ReportTemplateEntity> findFirstByInstitutionIdAndIsDefaultTrue(String institutionId);
    Optional<ReportTemplateEntity> findFirstByReportTypeAndIsDefaultTrue(ReportType reportType);
    Optional<ReportTemplateEntity> findFirstByReportTypeAndInstitutionIdAndIsDefaultTrue(ReportType reportType, String institutionId);
    List<ReportTemplateEntity> findByReportType(ReportType reportType);
    List<ReportTemplateEntity> findByInstitutionId(String institutionId);
}
