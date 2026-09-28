package com.simulator112.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.simulator112.auth.domain.model.Role;

@Data
public class ChangeRoleRequest {
    @NotNull(message = "Роль обязательна")
    private Role role;
}
