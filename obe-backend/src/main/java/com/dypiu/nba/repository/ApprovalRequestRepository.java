package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalRequest;
import com.dypiu.nba.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {
    List<ApprovalRequest> findBySchoolId(String schoolId);
    List<ApprovalRequest> findByProgrammeId(String programmeId);
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
}
