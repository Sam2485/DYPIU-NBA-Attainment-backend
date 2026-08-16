package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseMappingKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseMappingKeywordRepository extends JpaRepository<CourseMappingKeyword, String> {
    Optional<CourseMappingKeyword> findByCourseOfferingIdAndKeywordType(String courseOfferingId, String keywordType);
    List<CourseMappingKeyword> findByCourseOfferingId(String courseOfferingId);
    void deleteByCourseOfferingId(String courseOfferingId);
}
