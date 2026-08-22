package com.dypiu.nba.repository;

import com.dypiu.nba.entity.StudentCoMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StudentCoMarkRepository extends JpaRepository<StudentCoMark, String> {
    List<StudentCoMark> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<StudentCoMark> findByProgrammeBatchCourseIdAndCoCode(String programmeBatchCourseId, String coCode);
    List<StudentCoMark> findByProgrammeBatchCourseIdIn(Collection<String> programmeBatchCourseIds);
    List<StudentCoMark> findByProgrammeBatchCourseIdAndStudentId(String programmeBatchCourseId, String studentId);

    @Modifying
    @Query("DELETE FROM StudentCoMark s WHERE s.programmeBatchCourseId = :programmeBatchCourseId")
    void deleteByProgrammeBatchCourseId(@Param("programmeBatchCourseId") String programmeBatchCourseId);
}
