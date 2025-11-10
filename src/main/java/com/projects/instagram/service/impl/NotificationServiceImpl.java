package com.projects.instagram.service.impl;

import com.projects.instagram.broadcast.SseEmitterRegistry;
import com.projects.instagram.dto.CreateNotificationRequest;
import com.projects.instagram.dto.NotificationDto;
import com.projects.instagram.entity.Notification;
import com.projects.instagram.entity.NotificationType;
import com.projects.instagram.entity.User;
import com.projects.instagram.repository.NotificationRepository;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.NotificationService;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate; // for websocket push
    private final ModelMapper modelMapper = new ModelMapper();
    private final UserReposotory userReposotory;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   SimpMessagingTemplate messagingTemplate,
                                   UserReposotory userReposotory,
                                   SseEmitterRegistry sseEmitterRegistry
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.userReposotory = userReposotory;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // centralized save + publish helper
    private NotificationDto saveAndPublish(Notification n) {
        Notification saved = null;
//        try {
        saved = notificationRepository.save(n);
//        } catch (RuntimeException ex) {
//        catch (DataIntegrityViolationException ex) {
        // Duplicate (concurrent insert) — fetch existing and return its DTO
//            log.debug("Duplicate notification prevented by DB constraint: {}", ex.getMessage());
        // try to load existing notification (best-effort)
//            List<Notification> existing = notificationRepository.findExistingNotification(
//                    n.getRecipientId(), n.getActorId(), n.getType(), n.getPostId());
//            if (!existing.isEmpty()) {
//                saved = existing.get(0);
//            } else {
        // fallback: rethrow
//                throw ex;
//            }
//        }

        NotificationDto dto = modelMapper.map(saved, NotificationDto.class);

        // populate actor information
        userReposotory.findById(saved.getActorId()).ifPresent(actor -> {
            dto.setName(actor.getName());
            dto.setUsername(actor.getUsername());
            dto.setBio(actor.getBio());
            dto.setProfilePhotoUrl(actor.getProfilePhotoUrl());
        });

        // WebSocket push (STOMP)
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(saved.getRecipientId()),
                    "/queue/notifications",
                    dto);
        } catch (Exception e) {
            log.warn("WebSocket push failed for recipient {}: {}", saved.getRecipientId(), e.getMessage());
        }

        // SSE push
        try {
            sseEmitterRegistry.broadcast(saved.getRecipientId(), dto);
        } catch (Exception e) {
            log.warn("SSE broadcast failed for recipient {}: {}", saved.getRecipientId(), e.getMessage());
        }

        return dto;
    }

    @Override
    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request) {
        Notification n = new Notification();
        n.setRecipientId(request.getRecipientId());
        n.setActorId(request.getActorId());
        n.setType(request.getType());
        n.setTitle(request.getTitle());
        n.setMessage(request.getMessage());
        n.setLink(request.getLink());
        n.setMetadata(request.getMetadata());
        Notification saved = notificationRepository.save(n);

        NotificationDto dto = modelMapper.map(saved, NotificationDto.class);

        // -- FETCH ACTOR (not recipient) to populate who did the action
        User actor = userReposotory.findById(request.getActorId())
                .orElseThrow(() -> new IllegalArgumentException("Actor user not found"));

        dto.setName(actor.getName());
        dto.setUsername(actor.getUsername());
        dto.setBio(actor.getBio());
        dto.setProfilePhotoUrl(actor.getProfilePhotoUrl());

        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(request.getRecipientId()),
                    "/queue/notifications",
                    dto
            );
        } catch (Exception e) {
            log.warn("WebSocket push failed for recipient {}: {}", request.getRecipientId(), e.getMessage());
        }
        return dto;
    }

    @Override
    public Page<NotificationDto> getNotifications(Long recipientId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);

        List<NotificationDto> dtos = page.getContent().stream()
                .map(n -> {

                    NotificationDto dto = modelMapper.map(n, NotificationDto.class);

                    User user = userReposotory.findById(n.getActorId())
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));

                    dto.setName(user.getName());
                    dto.setUsername(user.getUsername());
                    dto.setBio(user.getBio());
                    dto.setProfilePhotoUrl(user.getProfilePhotoUrl());

                    return dto;
                })
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public List<Long> getAllNotificationsByPostId(Long postId) {
        List<Notification> notifications = notificationRepository.findAllByPostId(postId);

        return notifications.stream()
                .map(Notification::getRecipientId)  // extract each notification ID
                .collect(Collectors.toList());  // gather into a list
    }

    @Override
    public long countUnread(Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    //    @Override
//    @Transactional
//    public NotificationDto markAsRead(Long notificationId, Long recipientId) {
//        Notification n = notificationRepository.findById(notificationId)
//                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
//        if (!n.getRecipientId().equals(recipientId)) {
//            throw new IllegalArgumentException("Not allowed");
//        }
//        if (!n.isRead()) {
//            n.setRead(true);
//            notificationRepository.save(n);
//        }
//        return modelMapper.map(n, NotificationDto.class);
//    }
    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId, Long recipientId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!n.getRecipientId().equals(recipientId)) {
            throw new IllegalArgumentException("Not allowed");
        }
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
        }

        NotificationDto dto = modelMapper.map(n, NotificationDto.class);
        userReposotory.findById(n.getActorId()).ifPresent(actor -> {
            dto.setName(actor.getName());
            dto.setUsername(actor.getUsername());
            dto.setBio(actor.getBio());
            dto.setProfilePhotoUrl(actor.getProfilePhotoUrl());
        });

        return dto;
    }

    @Override
    @Transactional
    public void markAllAsRead(Long recipientId) {
        notificationRepository.markAllReadByRecipientId(recipientId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long recipientId) {
        if (recipientId == null)
            throw new IllegalArgumentException("recipientId required");
        // bulk delete; notificationRepository returns number of rows deleted (can be 0)
        try {
            notificationRepository.deleteByRecipientId(recipientId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete notifications for recipient " + recipientId, e);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long recipientId) {
        if (notificationId == null)
            throw new IllegalArgumentException("notificationId required");
        if (recipientId == null)
            throw new IllegalArgumentException("recipientId required");

        int deleted = 0;
        try {
            deleted = notificationRepository.deleteByIdAndRecipientId(notificationId, recipientId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete notification " + notificationId, e);
        }

        if (deleted == 0) {
            // nothing deleted — either not found or not owned by recipient
            throw new IllegalArgumentException("Notification not found or not owned by user");
        }
    }

    @Override
    @Transactional
    public void handleDeletionNotificationByUserIdAndRecipientIdAndType(Long userId, Long recipientId, String type) {
        if (userId == null)
            throw new IllegalArgumentException("userId required");
        if (recipientId == null)
            throw new IllegalArgumentException("recipientId required");
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("type required");

        // Convert string (like "LIKE") to enum safely
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid notification type: " + type);
        }

        notificationRepository.deleteByRecipientIdAndActorIdAndType(recipientId, userId, notificationType);
    }


    @Override
    public void createNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, String message, Long postId) {
        if (recipientId == null)
            throw new IllegalArgumentException("recipientId required");
        if (actorId == null)
            throw new IllegalArgumentException("actor required");
        if (type == null)
            throw new IllegalArgumentException("Notification type required");

        if (recipientId.equals(actorId))
            return; // don't notify for self-like

        if (postId == null) {

            if (!notificationRepository.existsByRecipientIdAndActorIdAndType(recipientId, actorId, type)) {
                Notification n = new Notification();
                n.setRecipientId(recipientId);
                n.setActorId(actorId);
                n.setType(type);
                n.setMessage(message);

                notificationRepository.save(n);
            }
        } else {
            if (!notificationRepository.existsByRecipientIdAndActorIdAndTypeAndPostId(recipientId, actorId, type, postId)) {
                Notification n = new Notification();
                n.setRecipientId(recipientId);
                n.setActorId(actorId);
                n.setType(type);
                n.setMessage(message);
                n.setPostId(postId);

                notificationRepository.save(n);
            }
        }
    }

    @Override
    public void createNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, String message) {
        createNotificationWithTypeIfNeeded(recipientId, actorId, type, message, null);
    }

    @Override
    public void deleteNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type, Long postId) {
        if (recipientId == null || actorId == null)
            return;

        notificationRepository.deleteByRecipientIdAndActorIdAndTypeAndPostId(
                recipientId, actorId, type, postId);
    }

    @Override
    public void deleteNotificationWithTypeIfNeeded(Long recipientId, Long actorId, NotificationType type) {
        if (recipientId == null || actorId == null)
            return;

        notificationRepository.deleteByRecipientIdAndActorIdAndType(
                recipientId, actorId, type);
    }

}