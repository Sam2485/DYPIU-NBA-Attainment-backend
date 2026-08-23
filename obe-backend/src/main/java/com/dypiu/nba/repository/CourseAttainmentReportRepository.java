package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseAttainmentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAttainmentReportRepository extends JpaRepository<CourseAttainmentReport, String> {
    Optional<CourseAttainmentReport> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<CourseAttainmentReport> findByProgrammeBatchCourseIdIn(List<String> programmeBatchCourseIds);
}
