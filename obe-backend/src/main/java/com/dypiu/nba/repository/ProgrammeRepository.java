package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, String> {
    List<Programme> findByDepartmentId(String departmentId);
}
