package com.amir.shop.service;

import com.amir.shop.dto.ProductCreateRequest;
import com.amir.shop.dto.ProductUpdateRequest;
import com.amir.shop.entity.Category;
import com.amir.shop.entity.Product;
import com.amir.shop.repository.CategoryRepository;
import com.amir.shop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository repository, CategoryRepository categoryRepository) {
        this.productRepository = repository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product createProduct(ProductCreateRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Категория не найдена")
                );

        Optional<Product> existingProduct =
                productRepository.findByNameIgnoreCase(request.getName());

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();

            if (product.getCategory() == null ||
                    !product.getCategory().getId().equals(category.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Товар с таким названием уже существует в другой категории "
                                + "или пока не имеет категории"
                );
            }
            product.setStock(product.getStock() + request.getStock());
            return productRepository.save(product);
        }

        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getImageUrl()
        );

        product.setCategory(category);
        return productRepository.save(product);
    }

    public Product getProductById(int id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return product.get();
    }

    public Product deleteProductById(int id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
        return product.get();
    }

    public Product updateProductById(ProductUpdateRequest request, int id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        product.get().setName(request.getName());
        product.get().setDescription(request.getDescription());
        product.get().setPrice(request.getPrice());
        product.get().setStock(request.getStock());
        product.get().setImageUrl(request.getImageUrl());

        return productRepository.save(product.get());
    }

    public Page<Product> searchProductsByName(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Page<Product> filterProductsByPrice(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable

    ) {
        return productRepository.findByPriceGreaterThanEqualAndPriceLessThanEqual(
                minPrice,
                maxPrice,
                pageable

        );
    }
}
