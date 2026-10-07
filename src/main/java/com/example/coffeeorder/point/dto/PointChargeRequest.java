package com.example.coffeeorder.point.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointChargeRequest(
        @NotBlank String userId,
        @NotNull @Positive Long amount
) {
}