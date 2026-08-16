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
    List<StudentCoMark> findByCourseOfferingId(String courseOfferingId);
    List<StudentCoMark> findByCourseOfferingIdAndCoCode(String courseOfferingId, String coCode);
    List<StudentCoMark> findByCourseOfferingIdIn(Collection<String> courseOfferingIds);

    @Modifying
    @Query("DELETE FROM StudentCoMark s WHERE s.courseOfferingId = :courseOfferingId")
    void deleteByCourseOfferingId(@Param("courseOfferingId") String courseOfferingId);
}
