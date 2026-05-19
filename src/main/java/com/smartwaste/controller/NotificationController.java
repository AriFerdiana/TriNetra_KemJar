package com.smartwaste.controller;

import com.smartwaste.entity.Notification;
import com.smartwaste.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasRole('CITIZEN')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(auth.getName()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(Authentication auth) {
        notificationService.markAllAsRead(auth.getName());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
