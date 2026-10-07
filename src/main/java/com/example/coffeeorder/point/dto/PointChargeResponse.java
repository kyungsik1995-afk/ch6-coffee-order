package com.example.coffeeorder.point.dto;

import com.example.coffeeorder.point.entity.PointAccount;

public record PointChargeResponse(
        String userId,
        Long chargedAmount,
        Long balance
) {

    public static PointChargeResponse from(
            PointAccount account,
            Long chargedAmount
    ) {
        return new PointChargeResponse(
                account.getUserId(),
                chargedAmount,
                account.getBalance()
        );
    }
}