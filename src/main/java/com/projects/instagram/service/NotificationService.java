package com.projects.instagram.service;

import com.projects.instagram.dto.CreateNotificationRequest;
import com.projects.instagram.dto.NotificationDto;
import com.projects.instagram.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationDto createNotification(CreateNotificationRequest request);

    Page<NotificationDto> getNotifications(Long recipientId, Pageable pageable);

    List<Long> getAllNotificationsByPostId(Long postId);

    long countUnread(Long recipientId);

    NotificationDto markAsRead(Long notificationId, Long recipientId);

    void markAllAsRead(Long recipientId);

    void deleteAllNotifications(Long recipientId);

    void deleteNotification(Long notificationId, Long recipientId);

    void handleDeletionNotificationByUserIdAndRecipientIdAndType(Long userId, Long recipientId, String type);


    void createNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, String message, Long postId);

    void createNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, String message);


    void deleteNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, Long postId);

    void deleteNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type);

}