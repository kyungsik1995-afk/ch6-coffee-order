package com.example.coffeeorder.point.controller;

import com.example.coffeeorder.point.dto.PointChargeRequest;
import com.example.coffeeorder.point.dto.PointChargeResponse;
import com.example.coffeeorder.point.entity.PointAccount;
import com.example.coffeeorder.point.service.PointService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/charge")
    public PointChargeResponse charge(
            @Valid @RequestBody PointChargeRequest request
    ) {
        PointAccount account = pointService.charge(
                request.userId(),
                request.amount()
        );

        return PointChargeResponse.from(
                account,
                request.amount()
        );
    }
}