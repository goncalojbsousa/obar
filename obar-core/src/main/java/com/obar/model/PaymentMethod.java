package com.obar.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"PaymentMethod\"")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "\"clientId\"", nullable = false)
    private User client;

    @Column(nullable = false)
    private String type;

    private String details;

    @Column(nullable = false)
    private Boolean active = true;
}