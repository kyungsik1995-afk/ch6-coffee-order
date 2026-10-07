package com.example.coffeeorder.menu.dto;

import com.example.coffeeorder.menu.entity.CoffeeMenu;

public class CoffeeMenuResponse {

    private final Long menuId;
    private final String name;
    private final Long price;

    public CoffeeMenuResponse(Long menuId, String name, Long price) {
        this.menuId = menuId;
        this.name = name;
        this.price = price;
    }

    public static CoffeeMenuResponse from(CoffeeMenu coffeeMenu) {
        return new CoffeeMenuResponse(
                coffeeMenu.getId(),
                coffeeMenu.getName(),
                coffeeMenu.getPrice()
        );
    }

    public Long getMenuId() {
        return menuId;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price;
    }
}