package com.dypiu.nba.repository;

import com.dypiu.nba.entity.MasterProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MasterProgrammeRepository extends JpaRepository<MasterProgramme, String> {
    List<MasterProgramme> findByDepartmentId(String departmentId);
    List<MasterProgramme> findByDepartmentIdIn(List<String> departmentIds);
    Optional<MasterProgramme> findByCode(String code);
}
