package com.dypiu.nba.reports.repository;

import com.dypiu.nba.reports.model.ReportAssetEntity;
import com.dypiu.nba.reports.model.ReportAssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportAssetRepository extends JpaRepository<ReportAssetEntity, String> {
    List<ReportAssetEntity> findByInstitutionId(String institutionId);
    List<ReportAssetEntity> findByInstitutionIdAndAssetType(String institutionId, ReportAssetType assetType);
    List<ReportAssetEntity> findByAssetType(ReportAssetType assetType);
}
