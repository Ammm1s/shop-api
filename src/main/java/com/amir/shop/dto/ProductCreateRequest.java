package com.amir.shop.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductCreateRequest {

    @NotNull(message = "Укажите категорию")
    @Positive(message = "ID категории должен быть положительным")
    private Integer categoryId;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    private String description;

    @NotNull(message = "Число не может быть пустым")
    @Positive(message = "Число должно быть положительным")
    private BigDecimal price;

    @NotNull(message = "Число не может быть пустым")
    @Min(value = 0, message = "Число должно быть неотрицательным")
    private Integer stock;

    public ProductCreateRequest() {
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public BigDecimal getPrice() {
        return price;
    }
    public int getStock() {
        return stock;
    }
    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
}