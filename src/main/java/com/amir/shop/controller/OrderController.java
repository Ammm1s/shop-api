package com.amir.shop.controller;

import com.amir.shop.dto.request.OrderRequest;
import com.amir.shop.dto.response.OrderResponse;
import com.amir.shop.dto.request.OrderStatusUpdateRequest;
import com.amir.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(
            @PathVariable Integer id,
            Principal principal
    ) {
        return service.getOrderById(id, principal.getName());
    }

    @GetMapping()
    public Page<OrderResponse> getUserOrders(
            Principal principal,
            Pageable pageable
    ) {
        return service.getUserOrders(principal.getName(), pageable);
    }

    @GetMapping("/admin")
    public Page<OrderResponse> getAllOrders(
            Pageable pageable) {
        return service.getAllOrders(pageable);
    }

    @PostMapping
    public OrderResponse createOrder(
            @Valid @RequestBody OrderRequest request,
            Principal principal
    ) {
        return service.createOrder(request, principal.getName());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable int id,
            Principal principal
     ) {
        return service.cancelOrder(id, principal.getName());
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Integer id,
            @RequestBody @Valid OrderStatusUpdateRequest request) {
        return service.updateStatus(id, request.getStatus());
    }

}
