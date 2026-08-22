package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseAtr;
import com.dypiu.nba.entity.CourseAtrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAtrRepository extends JpaRepository<CourseAtr, String> {
    List<CourseAtr> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<CourseAtr> findByProgrammeBatchCourseIdIn(Collection<String> programmeBatchCourseIds);
    Optional<CourseAtr> findByProgrammeBatchCourseIdAndCoCode(String programmeBatchCourseId, String coCode);
    List<CourseAtr> findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, CourseAtrStatus status);
}
