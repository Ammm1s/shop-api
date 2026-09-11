package com.amir.shop.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Column(unique = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private BigDecimal totalPrice;
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItem> items;

    public Order(){
    }

    public Order(BigDecimal totalPrice, List<OrderItem> items) {
        this.totalPrice = totalPrice;
        this.items = items;
    }

    @PrePersist
    public void beforeSave() {
        createdAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    public List<OrderItem> getItems() {
        return items;
    }
    public User getUser() {
        return user;
    }
    public OrderStatus getStatus() {
        return status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
