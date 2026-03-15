package com.obar.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "origin_address", nullable = false)
    private String originAddress;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    @Column(name = "origin_latitude")
    private Float originLatitude;

    @Column(name = "origin_longitude")
    private Float originLongitude;

    @Column(name = "destination_latitude")
    private Float destinationLatitude;

    @Column(name = "destination_longitude")
    private Float destinationLongitude;

    @Column(name = "distance_km")
    private Float distanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;
}