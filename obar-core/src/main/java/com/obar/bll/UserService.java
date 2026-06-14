package com.obar.bll;

import com.obar.bll.auth.PasswordService;
import com.obar.dal.UserRepository;
import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository = new UserRepository();
    private final PasswordService passwordService = new PasswordService();

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

    public User updateProfile(Integer userId, String name, String email, String phone, String taxNumber,
            String licenseNumber, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador nao encontrado."));
        String normalizedEmail = normalizeEmail(email);
        String normalizedTaxNumber = nullableText(taxNumber);
        String normalizedLicenseNumber = nullableText(licenseNumber);
        String safeNewPassword = text(newPassword);
        String safeConfirmPassword = text(confirmPassword);

        if (text(name).isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio.");
        }
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email e obrigatorio.");
        }
        if (!normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("Email invalido.");
        }
        if (phone != null && phone.trim().length() > 40) {
            throw new IllegalArgumentException("Telefone demasiado longo.");
        }
        if (user.getType() == UserType.DRIVER && normalizedLicenseNumber == null) {
            throw new IllegalArgumentException("Numero da carta e obrigatorio.");
        }
        if (!safeNewPassword.isBlank() || !safeConfirmPassword.isBlank()) {
            validatePasswordChange(safeNewPassword, safeConfirmPassword);
        }

        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email ja registado.");
                });
        if (normalizedTaxNumber != null) {
            userRepository.findByTaxNumber(normalizedTaxNumber)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("NIF ja registado.");
                    });
        }
        if (normalizedLicenseNumber != null) {
            userRepository.findByLicenseNumber(normalizedLicenseNumber)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Numero da carta ja registado.");
                    });
        }

        user.setName(text(name));
        user.setEmail(normalizedEmail);
        user.setPhone(nullableText(phone));
        user.setTaxNumber(normalizedTaxNumber);
        if (user.getType() == UserType.DRIVER) {
            user.setLicenseNumber(normalizedLicenseNumber);
        }
        if (!safeNewPassword.isBlank()) {
            user.setPasswordHash(passwordService.hash(safeNewPassword));
        }
        return userRepository.update(user);
    }

    public User requestDriverUpgrade(Integer userId, String phone, String licenseNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador nao encontrado."));
        String normalizedPhone = nullableText(phone);
        String normalizedLicenseNumber = nullableText(licenseNumber);

        if (user.getType() != UserType.CLIENT) {
            throw new IllegalArgumentException("Esta conta ja nao e uma conta cliente.");
        }
        if (normalizedLicenseNumber == null) {
            throw new IllegalArgumentException("Numero da carta e obrigatorio.");
        }
        if (normalizedPhone != null && normalizedPhone.length() > 40) {
            throw new IllegalArgumentException("Telefone demasiado longo.");
        }

        userRepository.findByLicenseNumber(normalizedLicenseNumber)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Numero da carta ja registado.");
                });

        user.setPhone(normalizedPhone);
        user.setLicenseNumber(normalizedLicenseNumber);
        user.setType(UserType.DRIVER);
        user.setStatus(AccountStatus.PENDING);
        user.setOnline(false);
        user.setAvailable(false);
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

    public void setDriverOnline(Integer driverId, boolean online) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista não encontrado."));
        if (driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Utilizador não é um condutor.");
        }
        if (online && (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null)) {
            throw new IllegalStateException("Ativa a localização para ficares online.");
        }

        driver.setOnline(online);
        driver.setAvailable(online);
        userRepository.update(driver);
    }

    public boolean hasOnlineAvailableDriverForCategory(String vehicleCategory) {
        return !userRepository.findAvailableDriversByVehicleCategoryWithCurrentLocation(vehicleCategory).isEmpty();
    }

    public void updateDriverCurrentLocation(Integer driverId, float currentLatitude, float currentLongitude) {
        userRepository.findById(driverId).ifPresent(u -> {
            if (u.getType() != UserType.DRIVER) {
                throw new IllegalArgumentException("Utilizador não é um condutor.");
            }
            u.setCurrentLatitude(currentLatitude);
            u.setCurrentLongitude(currentLongitude);
            u.setLastLocationUpdate(LocalDateTime.now());
            userRepository.update(u);
        });
    }

    public List<User> findOnlineDriversWithCurrentLocation() {
        return userRepository.findOnlineDriversWithCurrentLocation();
    }

    private static void validatePasswordChange(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("As palavras-passe nao coincidem.");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password deve ter pelo menos 8 caracteres.");
        }
        if (newPassword.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password deve conter pelo menos um numero.");
        }
        if (newPassword.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra maiuscula.");
        }
        if (newPassword.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra minuscula.");
        }
        if (newPassword.chars().noneMatch(character -> !Character.isLetterOrDigit(character))) {
            throw new IllegalArgumentException("Password deve conter pelo menos um caractere especial.");
        }
    }

    private static String normalizeEmail(String email) {
        return text(email).toLowerCase(Locale.ROOT);
    }

    private static String nullableText(String value) {
        String result = text(value);
        return result.isBlank() ? null : result;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
