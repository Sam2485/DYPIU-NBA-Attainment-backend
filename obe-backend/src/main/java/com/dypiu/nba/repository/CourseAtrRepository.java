package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseAtr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseAtrRepository extends JpaRepository<CourseAtr, String> {
    List<CourseAtr> findByCourseId(String courseId);
}
