package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department> findBySchoolId(String schoolId);
}
