package com.dypiu.nba.repository;

import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.deletion.DeletionRequestStatus;
import com.dypiu.nba.entity.DeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, Long>, JpaSpecificationExecutor<DeletionRequest> {
    
    Optional<DeletionRequest> findByResourceTypeAndResourceIdAndStatus(ResourceType resourceType, String resourceId, DeletionRequestStatus status);
    
    boolean existsByResourceTypeAndResourceIdAndStatus(ResourceType resourceType, String resourceId, DeletionRequestStatus status);
    
    List<DeletionRequest> findByStatusOrderByCreatedAtDesc(DeletionRequestStatus status);
    
    List<DeletionRequest> findByDepartmentIdAndStatusOrderByCreatedAtDesc(String departmentId, DeletionRequestStatus status);
    
    List<DeletionRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(String schoolId, DeletionRequestStatus status);
    
    List<DeletionRequest> findByRequestedByIdOrderByCreatedAtDesc(String requestedById);
}
