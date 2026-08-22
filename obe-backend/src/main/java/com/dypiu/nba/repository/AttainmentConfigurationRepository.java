package com.dypiu.nba.repository;

import com.dypiu.nba.entity.AttainmentConfigStatus;
import com.dypiu.nba.entity.AttainmentConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttainmentConfigurationRepository extends JpaRepository<AttainmentConfiguration, String> {
    Optional<AttainmentConfiguration> findByProgrammeBatchCourseId(String programmeBatchCourseId);
    List<AttainmentConfiguration> findByProgrammeBatchCourseIdIn(Collection<String> programmeBatchCourseIds);
    Optional<AttainmentConfiguration> findByProgrammeBatchCourseIdAndStatus(String programmeBatchCourseId, AttainmentConfigStatus status);
    void deleteByProgrammeBatchCourseId(String programmeBatchCourseId);
}
