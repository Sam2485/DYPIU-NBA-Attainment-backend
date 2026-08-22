package com.dypiu.nba.repository;

import com.dypiu.nba.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByResourceIdOrderByCreatedAtDesc(String resourceId);
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId);
}
