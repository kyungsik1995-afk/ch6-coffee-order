package com.example.coffeeorder.common.exception;

public record ErrorResponse(
        String code,
        String message
) {
}