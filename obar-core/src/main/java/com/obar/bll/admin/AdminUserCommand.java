package com.obar.bll.admin;

import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

/**
 * Immutable command payload used by admin user-management operations.
 */
public record AdminUserCommand(
                String name,
                String email,
                String phone,
                String reference,
                AccountStatus status,
                String password,
                UserType userType) {
}
