package com.backend.perfumes.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")


    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "perfume_id")
    private Perfume perfume;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false)
    private String opinion;

    @Column(nullable = false)
    private LocalDate date;

    @PrePersist
    public void prePersist() {
        if (date == null) {}
        date = LocalDate.now();
    }

}






