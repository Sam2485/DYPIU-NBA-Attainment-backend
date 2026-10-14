package com.dypiu.nba.repository;

import com.dypiu.nba.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("SELECT c FROM Conversation c WHERE (c.userOneId = :userOneId AND c.userTwoId = :userTwoId)")
    Optional<Conversation> findByUserPair(@Param("userOneId") Long userOneId, @Param("userTwoId") Long userTwoId);

    @Query("SELECT c FROM Conversation c WHERE c.userOneId = :userId OR c.userTwoId = :userId ORDER BY c.lastMessageAt DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId);
}
