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

    List<MasterProgramme> findByDeletedAtIsNull();
    List<MasterProgramme> findByDepartmentIdAndDeletedAtIsNull(String departmentId);
    List<MasterProgramme> findByDepartmentIdInAndDeletedAtIsNull(List<String> departmentIds);
    List<MasterProgramme> findByLevelAndDeletedAtIsNull(String level);
    List<MasterProgramme> findByDepartmentIdAndLevelAndDeletedAtIsNull(String departmentId, String level);
    Optional<MasterProgramme> findByCodeAndDeletedAtIsNull(String code);
    Optional<MasterProgramme> findByIdAndDeletedAtIsNull(String id);
    List<MasterProgramme> findByIdInAndDeletedAtIsNull(Iterable<String> ids);
    boolean existsByCodeAndDeletedAtIsNull(String code);
    boolean existsByIdAndDeletedAtIsNull(String id);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(mp) > 0 FROM MasterProgramme mp JOIN Department d ON mp.departmentId = d.id WHERE d.schoolId = :schoolId AND LOWER(TRIM(mp.code)) = LOWER(TRIM(:code)) AND mp.deletedAt IS NULL AND mp.id != :excludeId")
    boolean existsByCodeInSchoolExcludeId(@org.springframework.data.repository.query.Param("schoolId") String schoolId, @org.springframework.data.repository.query.Param("code") String code, @org.springframework.data.repository.query.Param("excludeId") String excludeId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(mp) > 0 FROM MasterProgramme mp JOIN Department d ON mp.departmentId = d.id WHERE d.schoolId = :schoolId AND LOWER(TRIM(mp.name)) = LOWER(TRIM(:name)) AND mp.deletedAt IS NULL AND mp.id != :excludeId")
    boolean existsByNameInSchoolExcludeId(@org.springframework.data.repository.query.Param("schoolId") String schoolId, @org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("excludeId") String excludeId);
}
