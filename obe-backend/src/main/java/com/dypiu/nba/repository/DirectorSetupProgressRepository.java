package com.dypiu.nba.repository;

import com.dypiu.nba.entity.DirectorSetupProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DirectorSetupProgressRepository extends JpaRepository<DirectorSetupProgress, String> {
    Optional<DirectorSetupProgress> findBySchoolId(String schoolId);
}
