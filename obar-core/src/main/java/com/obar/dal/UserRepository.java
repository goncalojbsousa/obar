package com.obar.dal;

import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public class UserRepository extends BaseRepository<User, Integer> {

    public UserRepository() {
        super(User.class);
    }

    public Optional<User> findByEmail(String email) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResultOptional();
        }
    }

    public List<User> findByType(UserType type) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.type = :type", User.class)
                    .setParameter("type", type)
                    .list();
        }
    }

    public List<User> findAvailableDrivers() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.type = 'DRIVER' AND u.available = true", User.class)
                    .list();
        }
    }

    public List<User> findPendingDrivers() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.type = :type AND u.status = :status ORDER BY u.createdAt ASC",
                            User.class)
                    .setParameter("type", UserType.DRIVER)
                    .setParameter("status", AccountStatus.PENDING)
                    .list();
        }
    }
}