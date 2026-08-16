package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, String> {
    List<CourseOffering> findByBatchId(String batchId);
    List<CourseOffering> findByBatchIdIn(Collection<String> batchIds);
    List<CourseOffering> findByCourseId(String courseId);
    Optional<CourseOffering> findByBatchIdAndCourseIdAndSemester(String batchId, String courseId, Integer semester);
    List<CourseOffering> findByCourseCoordinatorId(Long courseCoordinatorId);
    List<CourseOffering> findByCourseCoordinatorNameContainingIgnoreCaseOrAssignedFacultyContainingIgnoreCase(String name, String faculty);
}

