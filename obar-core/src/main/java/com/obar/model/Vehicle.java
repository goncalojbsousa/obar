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
@Table(name = "\"Vehicle\"")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "\"driverId\"", nullable = false)
    private User driver;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private String color;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    private Integer year;

    @Column(nullable = false)
    private String category;

    private BigDecimal baseFare;
    private BigDecimal pricePerKm;

    @Column(nullable = false)
    private Boolean active = true;
}