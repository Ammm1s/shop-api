package com.amir.shop.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequest {

    @NotBlank(message = "Название категории не может быть пустым")
    private String name;

    private String description;

    public CategoryRequest() {
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
}
