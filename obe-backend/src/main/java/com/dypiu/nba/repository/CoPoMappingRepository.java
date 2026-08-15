package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CoPoMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoPoMappingRepository extends JpaRepository<CoPoMapping, String> {
    List<CoPoMapping> findByCourseOutcomeId(String courseOutcomeId);
    List<CoPoMapping> findByCourseOutcomeIdIn(List<String> courseOutcomeIds);
    void deleteByCourseOutcomeId(String courseOutcomeId);
    void deleteByCourseOutcomeIdIn(List<String> courseOutcomeIds);
}
