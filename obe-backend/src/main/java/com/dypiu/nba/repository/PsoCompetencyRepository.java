package com.dypiu.nba.repository;

import com.dypiu.nba.entity.PsoCompetency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PsoCompetencyRepository extends JpaRepository<PsoCompetency, String> {
    List<PsoCompetency> findByPsoId(String psoId);
    List<PsoCompetency> findByPsoIdOrderByCodeAsc(String psoId);
    void deleteByPsoId(String psoId);
}
