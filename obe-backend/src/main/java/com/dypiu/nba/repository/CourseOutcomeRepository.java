package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalStatus;
import com.dypiu.nba.entity.CourseOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOutcomeRepository extends JpaRepository<CourseOutcome, String> {
    List<CourseOutcome> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<CourseOutcome> findByProgrammeBatchCourseIdOrderByCodeAsc(String programmeBatchCourseId);
    List<CourseOutcome> findByProgrammeBatchCourseIdIn(Collection<String> programmeBatchCourseIds);
    List<CourseOutcome> findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, ApprovalStatus status);
    Optional<CourseOutcome> findByProgrammeBatchCourseIdAndCode(String programmeBatchCourseId, String code);
    void deleteByProgrammeBatchCourseId(String programmeBatchCourseId);
}
