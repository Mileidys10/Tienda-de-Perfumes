package com.backend.perfumes.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name= "clients")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor


public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastname;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private LocalDate registrationDate;

    @OneToMany(mappedBy = "client")
    private List<Order> Orders = new ArrayList<>();

    @OneToMany(mappedBy = "client")
    private List<Review> reviewList = new ArrayList<>();

}
