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
    List<ProgrammeBatchCourse> findByProgrammeBatchIdAndMasterCourseId(String programmeBatchId, String masterCourseId);
    Optional<ProgrammeBatchCourse> findByIdAndDeletedAtIsNull(String id);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdAndDeletedAtIsNull(String programmeBatchId);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdInAndDeletedAtIsNull(Collection<String> programmeBatchIds);
    List<ProgrammeBatchCourse> findByMasterCourseIdAndDeletedAtIsNull(String masterCourseId);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdAndMasterCourseIdAndDeletedAtIsNull(String programmeBatchId, String masterCourseId);
    Optional<ProgrammeBatchCourse> findFirstByProgrammeBatchIdAndMasterCourseId(String programmeBatchId, String masterCourseId);
    boolean existsByProgrammeBatchIdAndMasterCourseIdAndDeletedAtIsNull(String programmeBatchId, String masterCourseId);
    boolean existsByProgrammeBatchIdAndMasterCourseIdAndIdNotAndDeletedAtIsNull(String programmeBatchId, String masterCourseId, String id);
    default boolean existsByProgrammeBatchIdAndMasterCourseId(String programmeBatchId, String masterCourseId) {
        return existsByProgrammeBatchIdAndMasterCourseIdAndDeletedAtIsNull(programmeBatchId, masterCourseId);
    }
    default boolean existsByProgrammeBatchIdAndMasterCourseIdAndIdNot(String programmeBatchId, String masterCourseId, String id) {
        return existsByProgrammeBatchIdAndMasterCourseIdAndIdNotAndDeletedAtIsNull(programmeBatchId, masterCourseId, id);
    }
    List<ProgrammeBatchCourse> findByCourseCoordinatorId(Long courseCoordinatorId);
    List<ProgrammeBatchCourse> findByCourseCoordinatorNameContainingIgnoreCaseOrAssignedFacultyContainingIgnoreCase(String name, String faculty);
    List<ProgrammeBatchCourse> findByProgrammeBatchIdAndStatus(String programmeBatchId, String status);

    Optional<ProgrammeBatchCourse> findByProgrammeBatchIdAndCodeAndDeletedAtIsNull(String programmeBatchId, String code);
    Optional<ProgrammeBatchCourse> findFirstByProgrammeBatchIdAndCodeIgnoreCaseAndDeletedAtIsNull(String programmeBatchId, String code);
    boolean existsByProgrammeBatchIdAndCodeIgnoreCaseAndDeletedAtIsNull(String programmeBatchId, String code);
    boolean existsByProgrammeBatchIdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String programmeBatchId, String code, String id);
}
