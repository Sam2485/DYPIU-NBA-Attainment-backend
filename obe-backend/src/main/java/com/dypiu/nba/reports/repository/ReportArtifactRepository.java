package com.dypiu.nba.reports.repository;

import com.dypiu.nba.reports.model.ArtifactType;
import com.dypiu.nba.reports.model.ReportArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportArtifactRepository extends JpaRepository<ReportArtifactEntity, String> {
    List<ReportArtifactEntity> findByReportId(String reportId);
    Optional<ReportArtifactEntity> findByReportIdAndArtifactType(String reportId, ArtifactType artifactType);
    Optional<ReportArtifactEntity> findBySha256Checksum(String sha256Checksum);
}
