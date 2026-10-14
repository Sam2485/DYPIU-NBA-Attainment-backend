package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Suggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, String> {

    Page<Suggestion> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Suggestion> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Suggestion s WHERE s.recipientId = :recipientId AND s.readAt IS NULL")
    long countUnreadSuggestionsForUser(@Param("recipientId") Long recipientId);
}
