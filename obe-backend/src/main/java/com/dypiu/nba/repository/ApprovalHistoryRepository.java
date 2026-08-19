package com.dypiu.nba.repository;

import com.dypiu.nba.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, String> {
    List<ApprovalHistory> findByApprovalRequestId(String approvalRequestId);
}
