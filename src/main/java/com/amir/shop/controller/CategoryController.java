package com.amir.shop.controller;

import com.amir.shop.dto.CategoryRequest;
import com.amir.shop.entity.Category;
import com.amir.shop.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return service.getAllCategories();
    }

    @PostMapping
    public Category createCategory(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }
}
