package com.smartwaste.service;

import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.Notification;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CitizenRepository citizenRepository;

    public NotificationService(NotificationRepository notificationRepository, CitizenRepository citizenRepository) {
        this.notificationRepository = notificationRepository;
        this.citizenRepository = citizenRepository;
    }

    @Transactional
    public Notification sendNotification(Citizen citizen, String title, String message, String type) {
        Notification notification = new Notification(citizen, title, message, type);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String email) {
        Citizen citizen = citizenRepository.findByEmail(email).orElse(null);
        if (citizen == null) return List.of();
        return notificationRepository.findByCitizenAndIsReadFalseOrderByCreatedAtDesc(citizen);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        Citizen citizen = citizenRepository.findByEmail(email).orElse(null);
        if (citizen != null) {
            List<Notification> unread = notificationRepository.findByCitizenAndIsReadFalseOrderByCreatedAtDesc(citizen);
            for (Notification n : unread) {
                n.setRead(true);
            }
            notificationRepository.saveAll(unread);
        }
    }

    @Transactional
    public void sendBroadcast(String title, String message, String type) {
        List<Citizen> allCitizens = citizenRepository.findAll();
        List<Notification> notifications = allCitizens.stream()
                .map(citizen -> new Notification(citizen, title, message, type))
                .toList();
        notificationRepository.saveAll(notifications);
    }
}
