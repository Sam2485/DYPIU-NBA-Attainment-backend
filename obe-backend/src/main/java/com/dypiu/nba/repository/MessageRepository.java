package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.recipientId = :recipientId AND m.readAt IS NULL")
    long countUnreadMessages(@Param("conversationId") String conversationId, @Param("recipientId") Long recipientId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientId = :recipientId AND m.readAt IS NULL")
    long countTotalUnreadMessagesForUser(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now WHERE m.conversationId = :conversationId AND m.recipientId = :recipientId AND m.readAt IS NULL")
    int markConversationAsRead(@Param("conversationId") String conversationId, @Param("recipientId") Long recipientId, @Param("now") ZonedDateTime now);
}
