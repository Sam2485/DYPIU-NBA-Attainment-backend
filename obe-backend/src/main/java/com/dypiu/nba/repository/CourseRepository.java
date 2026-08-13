package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByProgrammeId(String programmeId);
    List<Course> findByProgrammeIdAndAcademicYear(String programmeId, String academicYear);
}
