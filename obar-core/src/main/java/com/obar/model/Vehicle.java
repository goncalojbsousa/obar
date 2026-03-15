package com.obar.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private String color;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    private Integer year;

    @Column(nullable = false)
    private String category;

    @Column(name = "base_fare")
    private BigDecimal baseFare;

    @Column(name = "price_per_km")
    private BigDecimal pricePerKm;

    @Column(nullable = false)
    private Boolean active = true;
}