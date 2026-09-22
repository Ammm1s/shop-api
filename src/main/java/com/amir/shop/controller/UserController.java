package com.amir.shop.controller;

import com.amir.shop.dto.RoleUpdateRequest;
import com.amir.shop.dto.UserRequest;
import com.amir.shop.dto.UserResponse;
import com.amir.shop.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Validated
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

    @GetMapping
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return service.getAllUsers(pageable);
    }

    @DeleteMapping("/{id}")
    public UserResponse deleteUserById(@PathVariable @Positive Integer id) {
        return service.deleteUserById(id);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable @Positive Integer id, @Valid @RequestBody RoleUpdateRequest request) {
        return service.updateRole(id, request.getRole());
    }
}
