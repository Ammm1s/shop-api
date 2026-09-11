package com.amir.shop.controller;

import com.amir.shop.dto.ProductCreateRequest;
import com.amir.shop.dto.ProductUpdateRequest;
import com.amir.shop.entity.Product;
import com.amir.shop.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Validated
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Page<Product> getAllProducts(Pageable pageable) {
        return service.getAllProducts(pageable);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable @Positive int id) {
        return service.getProductById(id);
    }

    @GetMapping("/search")
    public Page<Product> searchProductsByName(@RequestParam String name, Pageable pageable) {
        return service.searchProductsByName(name, pageable);
    }

    @GetMapping("/filter")
    public Page<Product> filterProductsByPrice(

            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            Pageable pageable

    ) {
        return service.filterProductsByPrice(minPrice, maxPrice, pageable);
    }


    @PostMapping
    public Product createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return service.createProduct(request);
    }

    @DeleteMapping("/{id}")
    public Product deleteProductById(@PathVariable @Positive int id) {
        return service.deleteProductById(id);
    }

    @PutMapping("/{id}")
    public Product updateProductById(@PathVariable @Positive int id,
            @Valid @RequestBody ProductUpdateRequest request) {

        return service.updateProductById(request, id);
    }
}
