package com.projects.instagram.repository;

import com.projects.instagram.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // find all conversations where a user participates
    @Query("select c from Conversation c join c.participants p where p.id = :userId order by c.lastMessageAt desc nulls last, c.createdAt desc")
    List<Conversation> findByParticipant(@Param("userId") Long userId);
}
