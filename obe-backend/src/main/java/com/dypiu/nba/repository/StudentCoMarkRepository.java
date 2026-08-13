package com.dypiu.nba.repository;

import com.dypiu.nba.entity.StudentCoMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentCoMarkRepository extends JpaRepository<StudentCoMark, String> {
    List<StudentCoMark> findByCourseId(String courseId);
    List<StudentCoMark> findByCourseIdAndCoCode(String courseId, String coCode);
    void deleteByCourseId(String courseId);
}
