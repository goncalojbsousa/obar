package com.obar.dal;

import com.obar.model.Notification;
import org.hibernate.Session;

import java.util.List;

public class NotificationRepository extends BaseRepository<Notification, Integer> {

    public NotificationRepository() {
        super(Notification.class);
    }

    public List<Notification> findByUserId(Integer userId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC", Notification.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }

    public List<Notification> findUnreadByUserId(Integer userId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM Notification n WHERE n.user.id = :userId AND n.read = false", Notification.class)
                    .setParameter("userId", userId)
                    .list();
        }
    }
}