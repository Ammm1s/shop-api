package com.amir.shop.dto;

import com.amir.shop.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {

    private Instant createdAt;
    private OrderStatus status;
    private Integer id;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;

    public OrderResponse() {
    }

    public OrderResponse(Integer id, BigDecimal totalPrice, List<OrderItemResponse> items, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.totalPrice = totalPrice;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public OrderStatus getStatus() {
        return status;
    }
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}