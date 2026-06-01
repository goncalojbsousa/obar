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
                    "FROM User u WHERE u.type = :type AND u.available = true", User.class)
                    .setParameter("type", UserType.DRIVER)
                    .list();
        }
    }

    public List<User> findAvailableDriversByVehicleCategoryWithCurrentLocation(String vehicleCategory) {
        try (Session session = getSession()) {
            return session.createQuery(
                    "SELECT DISTINCT u "
                            + "FROM Vehicle v "
                            + "JOIN v.driver u "
                            + "WHERE u.type = :type "
                            + "AND u.status = :status "
                            + "AND u.available = true "
                            + "AND u.currentLatitude IS NOT NULL "
                            + "AND u.currentLongitude IS NOT NULL "
                            + "AND v.active = true "
                            + "AND upper(v.category) = upper(:vehicleCategory)",
                    User.class)
                    .setParameter("type", UserType.DRIVER)
                    .setParameter("status", AccountStatus.ACTIVE)
                    .setParameter("vehicleCategory", vehicleCategory)
                    .list();
        }
    }

    public List<User> findOnlineDriversWithCurrentLocation() {
        try (Session session = getSession()) {
            return session.createQuery(
                    "FROM User u "
                            + "WHERE u.type = :type "
                            + "AND u.status = :status "
                            + "AND u.available = true "
                            + "AND u.currentLatitude IS NOT NULL "
                            + "AND u.currentLongitude IS NOT NULL "
                            + "ORDER BY u.name ASC",
                    User.class)
                    .setParameter("type", UserType.DRIVER)
                    .setParameter("status", AccountStatus.ACTIVE)
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
