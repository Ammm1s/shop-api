package com.amir.shop.controller;

import com.amir.shop.dto.UserRequest;
import com.amir.shop.dto.UserResponse;
import com.amir.shop.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public UserResponse register(@Valid @RequestBody UserRequest request) {
        return service.register(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable @Positive Integer id) {
        return service.getUserById(id);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Principal principal) {
        return service.getCurrentUser(principal.getName());
    }
}
