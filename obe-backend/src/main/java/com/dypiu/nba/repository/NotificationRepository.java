package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    long countUnreadNotificationsForUser(@Param("recipientId") Long recipientId);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    int markAllAsReadForUser(@Param("recipientId") Long recipientId, @Param("now") ZonedDateTime now);
}
