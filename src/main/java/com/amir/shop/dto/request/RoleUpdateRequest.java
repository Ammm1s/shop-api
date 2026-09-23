package com.amir.shop.dto.request;

import com.amir.shop.entity.Role;
import jakarta.validation.constraints.NotNull;

public class RoleUpdateRequest {

    @NotNull(message = "Роль обязательна")
    private Role role;

    public RoleUpdateRequest() {
    }

    public Role getRole() {
        return role;
    }
}
