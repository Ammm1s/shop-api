package com.amir.shop.controller;

import com.amir.shop.dto.OrderRequest;
import com.amir.shop.dto.OrderResponse;
import com.amir.shop.service.OrderService;
import jakarta.validation.Valid;
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

}
