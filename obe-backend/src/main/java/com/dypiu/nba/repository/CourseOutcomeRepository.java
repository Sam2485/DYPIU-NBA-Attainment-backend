package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseOutcomeRepository extends JpaRepository<CourseOutcome, String> {
    List<CourseOutcome> findByCourseId(String courseId);
}
