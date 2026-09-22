package com.amir.shop.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

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

    @URL(protocol = "https", message = "Укажи HTTPS-ссылку на изображение")
    private String imageUrl;

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
    public String getImageUrl() {
        return imageUrl;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
}