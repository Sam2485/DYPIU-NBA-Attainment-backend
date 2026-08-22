package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ProgrammeBatchCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeBatchCourseRepository extends JpaRepository<ProgrammeBatchCourse, String> {
    List<ProgrammeBatchCourse> findByProgrammeBatchId(String programmeBatchId);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdIn(Collection<String> programmeBatchIds);
    List<ProgrammeBatchCourse> findByMasterCourseId(String masterCourseId);
    Optional<ProgrammeBatchCourse> findByProgrammeBatchIdAndMasterCourseIdAndSemester(String programmeBatchId, String masterCourseId, Integer semester);
    List<ProgrammeBatchCourse> findByCourseCoordinatorId(Long courseCoordinatorId);
    List<ProgrammeBatchCourse> findByCourseCoordinatorNameContainingIgnoreCaseOrAssignedFacultyContainingIgnoreCase(String name, String faculty);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdAndStatus(String programmeBatchId, String status);
}
