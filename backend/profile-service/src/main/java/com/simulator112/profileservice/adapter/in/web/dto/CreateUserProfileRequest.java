package com.simulator112.profileservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserProfileRequest(
        @NotBlank String name,
        @NotBlank String surname) {
}
