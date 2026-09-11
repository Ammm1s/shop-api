package com.amir.shop.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer quantity;

    private BigDecimal price;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Order order;

    public OrderItem() {
    }

    public OrderItem(Integer quantity, BigDecimal price, Product product, Order order) {
        this.quantity = quantity;
        this.price = price;
        this.product = product;
        this.order = order;
    }

    public Integer getId() {
        return id;
    }
    public Integer getQuantity() {
        return quantity;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public Product getProduct() {
        return product;
    }
    public Order getOrder() {
        return order;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
    public void setOrder(Order order) {
        this.order = order;
    }
}
