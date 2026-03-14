package com.obar.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"Review\"")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "\"tripId\"", nullable = false)
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "\"reviewerId\"", nullable = false)
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "\"reviewedId\"", nullable = false)
    private User reviewed;

    @Column(nullable = false)
    private Integer rating;

    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String reviewerType;
}