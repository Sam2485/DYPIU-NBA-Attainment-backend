package com.dypiu.nba.repository;

import com.dypiu.nba.entity.MasterCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MasterCourseRepository extends JpaRepository<MasterCourse, String> {
    List<MasterCourse> findByMasterProgrammeId(String masterProgrammeId);
    List<MasterCourse> findByMasterProgrammeIdIn(List<String> masterProgrammeIds);
    Optional<MasterCourse> findByMasterProgrammeIdAndCode(String masterProgrammeId, String code);
    List<MasterCourse> findByMasterProgrammeIdAndStatus(String masterProgrammeId, String status);
}
