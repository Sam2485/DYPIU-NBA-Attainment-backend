package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CoPsoMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoPsoMappingRepository extends JpaRepository<CoPsoMapping, String> {
    List<CoPsoMapping> findByCourseOutcomeId(String courseOutcomeId);
    List<CoPsoMapping> findByCourseOutcomeIdIn(List<String> courseOutcomeIds);
    void deleteByCourseOutcomeId(String courseOutcomeId);
    void deleteByCourseOutcomeIdIn(List<String> courseOutcomeIds);
}
