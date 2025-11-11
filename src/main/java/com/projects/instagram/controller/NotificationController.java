package com.projects.instagram.controller;

import com.projects.instagram.dto.CreateNotificationRequest;
import com.projects.instagram.dto.NotificationDto;
import com.projects.instagram.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "https://social-media-frontend-nbdo.vercel.app",
        "*"
})
@Slf4j
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    // in-memory SSE emitters (per user) — works for small-scale; use
    // Redis/clustered solution for prod.
    // private final CopyOnWriteArrayList<SseEmitter> emitters = new
    // CopyOnWriteArrayList<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Public paginated fetch (secured in prod)
    @GetMapping("/users/{userId}/notifications")
    public ResponseEntity<Page<NotificationDto>> getNotifications(@PathVariable Long userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
        } catch (Exception ex) {
            log.error("Error fetching notifications for user {} page {} size {}: ", userId, page, size, ex);
            throw ex; // or return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    // Internal API — create notification (call this from other services when you
    // want to notify a user)
    @PostMapping("/notifications")
    public ResponseEntity<NotificationDto> createNotification(@RequestBody CreateNotificationRequest req) {
        NotificationDto dto = notificationService.createNotification(req);
        return ResponseEntity.status(201).body(dto);
    }

    @GetMapping("/users/{userId}/notifications/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/users/{userId}/notifications/{notificationId}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long userId, @PathVariable Long notificationId) {
        NotificationDto dto = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/users/{userId}/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/users/{userId}/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable Long userId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {
        }

        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        var list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty())
                emitters.remove(userId);
        }
    }

    // delete single notification
    @DeleteMapping("/users/{userId}/notifications/{notificationId}")
    public ResponseEntity<Void> deleteNotificationEndpoint(@PathVariable Long userId, @PathVariable Long notificationId) {
        // validate authenticated user if needed
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // delete all notifications for user
    @DeleteMapping("/users/{userId}/notifications")
    public ResponseEntity<Void> deleteAllNotificationsEndpoint(@PathVariable Long userId) {
        // validate authenticated user if needed
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("users/{userId}/notifications/{recipientId}/by-type")
    public ResponseEntity<Void> deleteNotificationByUserIdAndRecipientId(@PathVariable Long userId,
                                                                         @PathVariable Long recipientId,
                                                                         @RequestParam String type) {
        notificationService.handleDeletionNotificationByUserIdAndRecipientIdAndType(userId, recipientId, type);
        return ResponseEntity.noContent().build();
    }

    public void broadcastToSseClients(Long recipientId, NotificationDto dto) {
        var list = emitters.get(recipientId);
        if (list == null)
            return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(dto));
            } catch (Exception e) {
                list.remove(emitter);
            }
        }
    }
}