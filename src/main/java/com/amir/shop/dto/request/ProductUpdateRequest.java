package com.amir.shop.dto.request;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public class ProductUpdateRequest {

    @NotBlank(message = "Название не может быть пустым")
    private String name;
    private String description;

    @NotNull(message = "Число не может быть пустым")
    @Positive(message = "Число должно быть положительным")
    private BigDecimal price;

    @NotNull(message = "Число не может быть пустым")
    @Min(value = 0, message = "Остаток должен быть неотрицательным")
    private Integer stock;

    @URL(protocol = "https", message = "Укажи HTTPS-ссылку на изображение")
    private String imageUrl;

    public ProductUpdateRequest() {
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
    public Integer getStock() {
        return stock;
    }
    public String getImageUrl() {
        return imageUrl;
    }
}
