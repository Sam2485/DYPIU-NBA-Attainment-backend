package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseAtr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAtrRepository extends JpaRepository<CourseAtr, String> {
    List<CourseAtr> findByCourseOfferingId(String courseOfferingId);
    List<CourseAtr> findByCourseOfferingIdIn(Collection<String> courseOfferingIds);
    Optional<CourseAtr> findByCourseOfferingIdAndCoCode(String courseOfferingId, String coCode);
}

