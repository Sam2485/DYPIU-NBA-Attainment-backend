package com.dypiu.nba.repository;

import com.dypiu.nba.entity.AttainmentConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AttainmentConfigurationRepository extends JpaRepository<AttainmentConfiguration, String> {
    Optional<AttainmentConfiguration> findByCourseId(String courseId);
}
