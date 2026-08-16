package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseMappingKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseMappingKeywordRepository extends JpaRepository<CourseMappingKeyword, String> {
    Optional<CourseMappingKeyword> findByCourseIdAndKeywordType(String courseId, String keywordType);
    List<CourseMappingKeyword> findByCourseId(String courseId);
    void deleteByCourseId(String courseId);
}
