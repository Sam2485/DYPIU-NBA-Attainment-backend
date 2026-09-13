package com.dypiu.nba.repository;

import com.dypiu.nba.entity.IqacAnalyticsConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IqacAnalyticsConfigurationRepository extends JpaRepository<IqacAnalyticsConfiguration, String> {
}
