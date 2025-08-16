package com.backend.perfumes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.tomcat.util.buf.UDecoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "clients")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;


    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private OrderStatus status;

@Column(nullable = false)
private BigDecimal total;

@ManyToOne(optional = false)
@JoinColumn(name="client_id")
private  Client client;

@ManyToOne(optional = false)
@JoinColumn(name="seller_id")
 private User seller;

@ManyToOne
@JoinColumn(name = "discount_id")
 private Discount discount;


    @Enumerated(EnumType.STRING)
    @OneToMany(mappedBy = "client")
    private List<Order> Orders = new ArrayList<>();

    @OneToMany(mappedBy = "client")
    private List<Review> reviewList = new ArrayList<>();

}
