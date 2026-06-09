package com.obar.bll.admin;

import com.obar.bll.auth.PasswordService;
import com.obar.dal.UserRepository;
import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Admin BLL use cases for users, clients, drivers, and driver approval.
 */
final class AdminUserManagementService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    AdminUserManagementService(UserRepository userRepository, PasswordService passwordService) {
        if (userRepository == null) {
            throw new IllegalArgumentException("UserRepository must not be null.");
        }
        if (passwordService == null) {
            throw new IllegalArgumentException("PasswordService must not be null.");
        }
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    List<AdminUserDTO> listPendingDrivers() {
        return userRepository.findPendingDrivers().stream()
                .map(AdminUserDTO::from)
                .toList();
    }

    List<AdminUserDTO> listUsersByType(UserType type) {
        if (type == null) {
            throw new IllegalArgumentException("User type is required.");
        }
        return userRepository.findByType(type).stream()
                .map(AdminUserDTO::from)
                .toList();
    }

    AdminUserDTO createUser(AdminUserCommand command) {
        validateRequiredFields(command);
        validatePassword(command.password(), true);

        String normalizedEmail = AdminTextSanitizer.normalizeEmail(command.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email ja registado.");
        }

        User user = new User();
        user.setName(command.name().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(AdminTextSanitizer.trimOrNull(command.phone()));
        user.setStatus(command.status());
        user.setType(command.userType());
        user.setPasswordHash(passwordService.hash(command.password()));
        user.setCreatedAt(LocalDateTime.now());

        applySectionSpecificData(user, command.reference(), command.userType());
        if (command.userType() == UserType.DRIVER && user.getStatus() != AccountStatus.ACTIVE) {
            user.setAvailable(false);
        }

        User savedUser = userRepository.save(user);
        return AdminUserDTO.from(savedUser);
    }

    AdminUserDTO updateUser(Integer userId, AdminUserCommand command) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }

        validateRequiredFields(command);
        validatePassword(command.password(), false);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));

        String normalizedEmail = AdminTextSanitizer.normalizeEmail(command.email());
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new IllegalArgumentException("Email ja registado.");
        }

        user.setName(command.name().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(AdminTextSanitizer.trimOrNull(command.phone()));
        user.setStatus(command.status());
        user.setType(command.userType());
        applySectionSpecificData(user, command.reference(), command.userType());

        if (command.password() != null && !command.password().isBlank()) {
            user.setPasswordHash(passwordService.hash(command.password()));
        }

        if (command.userType() == UserType.DRIVER && user.getStatus() != AccountStatus.ACTIVE) {
            user.setAvailable(false);
        }

        User updatedUser = userRepository.update(user);
        return AdminUserDTO.from(updatedUser);
    }

    void approveDriver(Integer userId, String approvalNote) {
        User user = findPendingDriverForDecision(userId);
        user.setStatus(AccountStatus.ACTIVE);
        user.setApprovalNote(AdminTextSanitizer.trimOrNull(approvalNote));
        userRepository.update(user);
    }

    void rejectDriver(Integer userId, String rejectionNote) {
        User user = findPendingDriverForDecision(userId);
        user.setStatus(AccountStatus.BLOCKED);
        user.setApprovalNote(AdminTextSanitizer.trimOrNull(rejectionNote));
        userRepository.update(user);
    }

    void blockUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));

        user.setStatus(AccountStatus.BLOCKED);
        if (user.getType() == UserType.DRIVER) {
            user.setAvailable(false);
        }
        userRepository.update(user);
    }

    void unblockUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));

        user.setStatus(AccountStatus.ACTIVE);
        if (user.getType() == UserType.DRIVER) {
            user.setAvailable(true);
        }
        userRepository.update(user);
    }

    void deleteUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));
        userRepository.deleteById(userId);
    }

    private User findPendingDriverForDecision(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
        if (user.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("O utilizador nao e um motorista.");
        }
        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Apenas contas pendentes podem ser aprovadas ou rejeitadas.");
        }
        return user;
    }

    private void validateRequiredFields(AdminUserCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados invalidos.");
        }

        String name = AdminTextSanitizer.safe(command.name());
        String email = AdminTextSanitizer.safe(command.email());
        String reference = AdminTextSanitizer.safe(command.reference());

        if (name.isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio.");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email e obrigatorio.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email invalido.");
        }
        if (command.status() == null) {
            throw new IllegalArgumentException("Estado e obrigatorio.");
        }
        if (command.userType() == null) {
            throw new IllegalArgumentException("Tipo de utilizador obrigatorio.");
        }
        if (command.phone() != null && command.phone().trim().length() > 40) {
            throw new IllegalArgumentException("Telefone demasiado longo.");
        }
        if (reference.isBlank()) {
            throw new IllegalArgumentException(command.userType() == UserType.DRIVER
                    ? "Numero de licenca e obrigatorio."
                    : "NIF e obrigatorio.");
        }
    }

    private void validatePassword(String password, boolean required) {
        if (!required && (password == null || password.isBlank())) {
            return;
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password e obrigatoria.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password deve conter pelo menos um numero.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra maiuscula.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra minuscula.");
        }
        boolean hasSpecialCharacter = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));
        if (!hasSpecialCharacter) {
            throw new IllegalArgumentException("Password deve conter pelo menos um caractere especial.");
        }
    }

    private void applySectionSpecificData(User user, String reference, UserType userType) {
        String safeReference = AdminTextSanitizer.safe(reference);
        if (userType == UserType.DRIVER) {
            user.setLicenseNumber(safeReference);
            user.setTaxNumber(null);
            if (user.getAvailable() == null) {
                user.setAvailable(user.getStatus() == AccountStatus.ACTIVE);
            }
            return;
        }

        user.setTaxNumber(safeReference);
        user.setLicenseNumber(null);
        user.setAvailable(false);
    }
}
