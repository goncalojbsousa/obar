package com.obar.bll.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password utility service responsible for secure hashing and verification
 *
 */
public class PasswordService {

    private static final int BCRYPT_COST = 12;

    /**
     * Hashes a plain-text password using BCrypt
     *
     * @param plainPassword plain-text password to hash
     * @return Bcrypt hash of the provided password
     * @throws IllegalArgumentException when {@code plainPassword} is {@code null} or blank
     */
    public String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash
     *
     * @param plainPassword plain-text password provided by the user
     * @param storedHash Bcrypt hash
     * @return {@code true} when the password matches the hash; {@code false} otherwise
     */
    public boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}