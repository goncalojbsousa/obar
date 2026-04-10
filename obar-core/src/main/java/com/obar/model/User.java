package com.obar.model;

import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.ClientTaxCategory;
import com.obar.model.enums.UserType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "account_status")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_type")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private UserType type;

    // Campos CLIENT
    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "default_payment_method_id")
    private Integer defaultPaymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", columnDefinition = "client_tax_category")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private ClientTaxCategory taxCategory;

    @ManyToOne
    @JoinColumn(name = "default_tax_rate_id")
    private TaxRate defaultTaxRate;

    @ManyToOne
    @JoinColumn(name = "default_currency_id")
    private Currency defaultCurrency;

    // Campos DRIVER
    @Column(name = "license_number")
    private String licenseNumber;

    private Boolean available = false;

    @Column(name = "average_rating")
    private Float averageRating = 0f;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;
}