package com.dypiu.nba.repository;

import com.dypiu.nba.entity.PoCompetency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoCompetencyRepository extends JpaRepository<PoCompetency, String> {
    List<PoCompetency> findByPoId(String poId);
    List<PoCompetency> findByPoIdOrderByCodeAsc(String poId);
    void deleteByPoId(String poId);
}
