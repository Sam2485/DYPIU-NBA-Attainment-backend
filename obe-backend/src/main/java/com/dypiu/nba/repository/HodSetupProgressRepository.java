package com.dypiu.nba.repository;

import com.dypiu.nba.entity.HodSetupProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HodSetupProgressRepository extends JpaRepository<HodSetupProgress, String> {
    Optional<HodSetupProgress> findByDepartmentId(String departmentId);
    Optional<HodSetupProgress> findByHodEmailIgnoreCase(String hodEmail);
}
