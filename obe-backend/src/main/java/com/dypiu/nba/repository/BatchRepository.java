package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {
    List<Batch> findByProgrammeId(String programmeId);
}
