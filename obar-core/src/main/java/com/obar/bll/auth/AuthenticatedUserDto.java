package com.obar.bll.auth;

import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

/**
 * Immutable authenticated user boundary object exposed to presentation layers
 *
 * @param id user identifier
 * @param name user full name
 * @param email user email address
 * @param type user role type
 * @param status current account status
 */
public record AuthenticatedUserDto(
        Integer id,
        String name,
        String email,
        UserType type,
        AccountStatus status) {

    /**
     * Creates an authenticated user DTO from a domain user entity
     *
     * @param user source user entity from the core domain
     * @return immutable DTO without password hash exposure
     */
    public static AuthenticatedUserDto from(User user) {
        return new AuthenticatedUserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getType(),
                user.getStatus());
    }
}