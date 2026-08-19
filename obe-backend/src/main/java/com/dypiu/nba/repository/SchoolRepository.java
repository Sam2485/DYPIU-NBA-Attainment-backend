package com.dypiu.nba.repository;

import com.dypiu.nba.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, String> {
    Optional<School> findByDirectorEmailIgnoreCase(String directorEmail);
    Optional<School> findByDirectorId(Long directorId);
}
