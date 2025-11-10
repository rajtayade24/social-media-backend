package com.projects.instagram.repository;


import com.projects.instagram.entity.Notification;
import com.projects.instagram.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndReadFalse(Long recipientId);

    List<Notification> findAllByPostId(Long postId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    boolean existsByRecipientIdAndActorIdAndTypeAndPostId(Long postOwnerId,
                                                          Long actorId,
                                                          NotificationType type,
                                                          Long postId);

    @Modifying
    boolean existsByRecipientIdAndActorIdAndType(Long targetId, Long userId, NotificationType notificationType);

    // bulk mark read
    @Modifying
    @Transactional
    @Query("update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);

    @Modifying
    @Transactional
    @Query("delete from Notification n where n.recipientId = :recipientId")
    int deleteByRecipientId(@Param("recipientId") Long recipientId);

    // new: delete single notification only if it belongs to recipient (returns number of rows deleted)
    @Modifying
    @Transactional
    @Query("delete from Notification n where n.id = :id and n.recipientId = :recipientId")
    int deleteByIdAndRecipientId(@Param("id") Long id, @Param("recipientId") Long recipientId);


    int deleteByRecipientIdAndActorIdAndType(Long recipientId, Long actorId, NotificationType type);

    int deleteByRecipientIdAndActorIdAndTypeAndPostId(
            Long recipientId,
            Long actorId,
            NotificationType type,
            Long postId
    );

    // helper to find existing notification in case of race handling fallback
    @Query("select n from Notification n where n.recipientId = :recipientId and n.actorId = :actorId and n.type = :type and ((:postId is null and n.postId is null) or n.postId = :postId)")
    List<Notification> findExistingNotification(@Param("recipientId") Long recipientId,
                                                @Param("actorId") Long actorId,
                                                @Param("type") NotificationType type,
                                                @Param("postId") Long postId);
}