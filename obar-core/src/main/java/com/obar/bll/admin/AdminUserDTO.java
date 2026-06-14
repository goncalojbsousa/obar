package com.obar.bll.admin;

import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;

import java.time.LocalDateTime;

public record AdminUserDTO(
        Integer id,
        String name,
        String email,
        String phone,
        UserType type,
        AccountStatus status,
        Float averageRating,
        Integer totalTrips,
        Boolean available,
        Boolean online,
        String photoUrl,
        String licenseNumber,
        String taxNumber,
        Integer defaultPaymentMethodId,
        LocalDateTime createdAt,
        String approvalNote) {

    public static AdminUserDTO from(User user) {
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
                user.getOnline(),
                user.getPhotoUrl(),
                user.getLicenseNumber(),
                user.getTaxNumber(),
                user.getDefaultPaymentMethodId(),
                user.getCreatedAt(),
                user.getApprovalNote());
    }
}
