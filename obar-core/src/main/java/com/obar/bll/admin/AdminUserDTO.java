package com.obar.bll.admin;

import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

import java.time.LocalDateTime;

/**
 * Read-only BLL DTO exposed to UI for admin user management screens.
 */
public final class AdminUserDTO {

    private final Integer id;
    private final String name;
    private final String email;
    private final String phone;
    private final UserType type;
    private final AccountStatus status;
    private final Float averageRating;
    private final Integer totalTrips;
    private final Boolean available;
    private final String licenseNumber;
    private final String taxNumber;
    private final Integer defaultPaymentMethodId;
    private final LocalDateTime createdAt;
    private final String approvalNote;

    private AdminUserDTO(
            Integer id,
            String name,
            String email,
            String phone,
            UserType type,
            AccountStatus status,
            Float averageRating,
            Integer totalTrips,
            Boolean available,
            String licenseNumber,
            String taxNumber,
            Integer defaultPaymentMethodId,
            LocalDateTime createdAt,
            String approvalNote) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.type = type;
        this.status = status;
        this.averageRating = averageRating;
        this.totalTrips = totalTrips;
        this.available = available;
        this.licenseNumber = licenseNumber;
        this.taxNumber = taxNumber;
        this.defaultPaymentMethodId = defaultPaymentMethodId;
        this.createdAt = createdAt;
        this.approvalNote = approvalNote;
    }

    public static AdminUserDTO from(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }

        return new AdminUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getType(),
                user.getStatus(),
                user.getAverageRating(),
                user.getTotalTrips(),
                user.getAvailable(),
                user.getLicenseNumber(),
                user.getTaxNumber(),
                user.getDefaultPaymentMethodId(),
                user.getCreatedAt(),
                user.getApprovalNote());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Float getAverageRating() {
        return averageRating;
    }

    public Integer getTotalTrips() {
        return totalTrips;
    }

    public Boolean getAvailable() {
        return available;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public Integer getDefaultPaymentMethodId() {
        return defaultPaymentMethodId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getApprovalNote() {
        return approvalNote;
    }
}
