package com.obar.bll;

import com.obar.dal.NotificationRepository;
import com.obar.model.Notification;
import com.obar.model.User;
import com.obar.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationService {

    private final NotificationRepository notificationRepository = new NotificationRepository();

    public Notification send(User user, String message, NotificationType type) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setType(type);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    public List<Notification> findByUser(Integer userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> findUnread(Integer userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public void markAsRead(Integer notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.update(n);
        });
    }
}