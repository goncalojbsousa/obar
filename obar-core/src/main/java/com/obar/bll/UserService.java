package com.obar.bll;

import com.obar.dal.UserRepository;
import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository userRepository = new UserRepository();

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public List<User> findAllDrivers() {
        return userRepository.findByType(UserType.DRIVER);
    }

    public List<User> findAvailableDrivers() {
        return userRepository.findAvailableDrivers();
    }

    public User update(User user) {
        return userRepository.update(user);
    }

    public void blockUser(Integer userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatus(AccountStatus.BLOCKED);
            userRepository.update(u);
        });
    }

    public void setDriverAvailability(Integer driverId, boolean available) {
        userRepository.findById(driverId).ifPresent(u -> {
            if (u.getType() != UserType.DRIVER) {
                throw new IllegalArgumentException("Utilizador não é um condutor.");
            }
            u.setAvailable(available);
            userRepository.update(u);
        });
    }
}