package com.amir.shop.dto.request;

import jakarta.validation.constraints.*;

public class OrderItemRequest {

    @NotNull(message = "ID товара не может быть пустым")
    @Positive(message = "ID товара должен быть положительным")
    private Integer productId;

    @NotNull(message = "Количество не может быть пустым")
    @Positive(message = "Количество должно быть положительным")
    private Integer quantity;

    public OrderItemRequest() {
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}