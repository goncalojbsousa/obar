package com.obar.model;

import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"Trip\"")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "\"clientId\"", nullable = false)
    private User client;

    @ManyToOne
    @JoinColumn(name = "\"driverId\"")
    private User driver;

    @ManyToOne
    @JoinColumn(name = "\"vehicleId\"")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "\"routeId\"", nullable = false)
    private Route route;

    @Column(nullable = false)
    private LocalDateTime requestTime = LocalDateTime.now();

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private TripStatus status = TripStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private TripType tripType;

    private String cancelledBy;
    private String cancelReason;
    private String notes;

    private BigDecimal estimatedPrice;
    private BigDecimal finalPrice;
}