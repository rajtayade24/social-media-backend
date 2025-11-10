package com.projects.instagram.repository;

import com.projects.instagram.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    long countByConversationIdAndReadByRecipientFalseAndSenderIdNot(Long conversationId, Long senderId);

    List<Message> findByConversationId(Long conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.readByRecipient = true " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.senderId <> :requesterId " +
            "AND m.readByRecipient = false")
    int markReadByConversationIdAndSenderIdNot(@Param("conversationId") Long conversationId,
                                               @Param("requesterId") Long requesterId);


    @Query("SELECT COUNT(DISTINCT m.conversation.id) " +
            "FROM Message m " +
            "JOIN m.conversation.participants p " +
            "WHERE p.id = :userId " +
            "  AND m.readByRecipient = false " +
            "  AND m.senderId <> :userId")
    long countDistinctConversationIdsWithUnreadForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) " +
            "FROM Message m " +
            "JOIN m.conversation.participants p " +
            "WHERE p.id = :userId " +
            "  AND m.readByRecipient = false " +
            "  AND m.senderId <> :userId")
    long countUnreadMessagesForUser(@Param("userId") Long userId);


}
