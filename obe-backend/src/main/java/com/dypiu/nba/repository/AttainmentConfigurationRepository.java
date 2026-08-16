package com.dypiu.nba.repository;

import com.dypiu.nba.entity.AttainmentConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttainmentConfigurationRepository extends JpaRepository<AttainmentConfiguration, String> {
    Optional<AttainmentConfiguration> findByCourseOfferingId(String courseOfferingId);
    List<AttainmentConfiguration> findByCourseOfferingIdIn(Collection<String> courseOfferingIds);
    void deleteByCourseOfferingId(String courseOfferingId);
}
