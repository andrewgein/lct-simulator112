package com.simulator112.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.simulator112.auth.model.enums.Role;

@Data
public class ChangeRoleRequest {
    @NotNull(message = "Роль обязательна")
    private Role role;
}
