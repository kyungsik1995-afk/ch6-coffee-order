package com.example.coffeeorder.menu.controller;

import com.example.coffeeorder.menu.dto.CoffeeMenuResponse;
import com.example.coffeeorder.menu.service.CoffeeMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class CoffeeMenuController {

    private final CoffeeMenuService coffeeMenuService;

    public CoffeeMenuController(CoffeeMenuService coffeeMenuService) {
        this.coffeeMenuService = coffeeMenuService;
    }

    @GetMapping
    public List<CoffeeMenuResponse> getMenus() {
        return coffeeMenuService.getMenus()
                .stream()
                .map(CoffeeMenuResponse::from)
                .toList();
    }
}