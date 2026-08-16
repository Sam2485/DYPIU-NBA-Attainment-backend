package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOutcomeRepository extends JpaRepository<CourseOutcome, String> {
    List<CourseOutcome> findByCourseOfferingId(String courseOfferingId);
    List<CourseOutcome> findByCourseOfferingIdOrderByCodeAsc(String courseOfferingId);
    List<CourseOutcome> findByCourseOfferingIdIn(Collection<String> courseOfferingIds);
    Optional<CourseOutcome> findByCourseOfferingIdAndCode(String courseOfferingId, String code);
    void deleteByCourseOfferingId(String courseOfferingId);
}
