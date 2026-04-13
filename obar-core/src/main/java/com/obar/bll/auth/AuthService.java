package com.obar.bll.auth;

import com.obar.dal.UserRepository;
import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

/**
 * Authentication and credential-management service
 *
 */

public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AuthService() {
        this(new UserRepository(), new PasswordService());
    }

    /**
     * Creates a new authentication service with required collaborators
     *
     * @param userRepository  repository used to load and update users
     * @param passwordService service used to hash and verify passwords
     */
    public AuthService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    /**
     * Registers a new user with secure password hashing
     *
     * @param name          user full name
     * @param email         user email address
     * @param plainPassword plain-text password provided at registration
     * @param type          role assigned to the new user
     * @return authenticated user DTO for the newly created user
     * @throws AuthenticationException when required values are missing or email is
     *                                 already registered
     */
    public AuthenticatedUserDto register(String name, String email, String plainPassword, UserType type) {
        String safeName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        if (safeName.isBlank()) {
            throw new AuthenticationException("Name must not be blank.");
        }
        if (normalizedEmail.isBlank()) {
            throw new AuthenticationException("Email must not be blank.");
        }
        if (type == null) {
            throw new AuthenticationException("User type must not be null.");
        }
        validatePasswordStrength(plainPassword, "Password");
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthenticationException("Email already registered.");
        }

        User user = new User();
        user.setName(safeName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordService.hash(plainPassword));
        user.setType(type);
        user.setStatus(AccountStatus.ACTIVE);

        User saved = userRepository.save(user);
        return AuthenticatedUserDto.from(saved);
    }

    /**
     * Authenticates a user by email and password
     *
     * @param email    user email
     * @param password plain-text password
     * @return authenticated user DTO when credentials are valid and account is
     *         active
     * @throws AuthenticationException when the email is unknown, password is
     *                                 invalid, or account status does not permit
     *                                 login
     */
    public AuthenticatedUserDto authenticate(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));

        validateActiveStatus(user.getStatus());

        if (passwordService.verify(password, user.getPasswordHash())) {
            return AuthenticatedUserDto.from(user);
        }

        throw new AuthenticationException("Invalid credentials.");
    }

    /**
     * Changes a user's password after validating current credentials
     *
     * @param userId          identifier of the user updating credentials
     * @param currentPassword current plain-text password for confirmation
     * @param newPassword     new plain-text password to persist
     * @throws AuthenticationException when the user does not exist, current
     *                                 password is incorrect, or the new password is
     *                                 blank or unchanged
     */
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials."));

        if (passwordService.verify(currentPassword, user.getPasswordHash())) {
            validatePasswordStrength(newPassword, "New password");

            if (currentPassword != null && currentPassword.equals(newPassword)) {
                throw new AuthenticationException("New password must differ from current.");
            }

            user.setPasswordHash(passwordService.hash(newPassword));
            userRepository.update(user);
            return;
        }

        throw new AuthenticationException("Current password is incorrect.");
    }

    private void validatePasswordStrength(String password, String label) {
        if (password == null || password.isBlank()) {
            throw new AuthenticationException(label + " must not be blank.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthenticationException(
                    label + " must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new AuthenticationException(label + " must contain at least one number.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new AuthenticationException(label + " must contain at least one uppercase letter.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new AuthenticationException(label + " must contain at least one lowercase letter.");
        }
        boolean hasSpecialCharacter = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasSpecialCharacter) {
            throw new AuthenticationException(label + " must contain at least one special character.");
        }
    }

    private void validateActiveStatus(AccountStatus status) {
        if (status == AccountStatus.ACTIVE) {
            return;
        }
        throw new AuthenticationException(switch (status) {
            case BLOCKED -> "Account is blocked.";
            case INACTIVE -> "Account is inactive.";
            case PENDING -> "Account is pending approval.";
            default -> "Account is not active.";
        });
    }
}