package com.amir.shop.service;

import com.amir.shop.dto.request.CategoryRequest;
import com.amir.shop.entity.Category;
import com.amir.shop.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public Category createCategory(CategoryRequest request) {

        Category category = new Category(

                request.getName(),
                request.getDescription()
        );

        return repository.save(category);
    }

    public List<Category> getAllCategories() {
        return repository.findAll();
    }

}
