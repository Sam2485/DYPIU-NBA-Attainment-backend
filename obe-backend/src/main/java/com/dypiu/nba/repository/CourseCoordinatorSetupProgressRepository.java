package com.dypiu.nba.repository;

import com.dypiu.nba.entity.CourseCoordinatorSetupProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CourseCoordinatorSetupProgressRepository extends JpaRepository<CourseCoordinatorSetupProgress, String> {
    Optional<CourseCoordinatorSetupProgress> findByCourseOfferingId(String courseOfferingId);
    Optional<CourseCoordinatorSetupProgress> findByCoordinatorEmailAndCourseOfferingId(String coordinatorEmail, String courseOfferingId);
}
