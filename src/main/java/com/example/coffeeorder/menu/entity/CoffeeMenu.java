package com.example.coffeeorder.menu.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "coffee_menu")
public class CoffeeMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long price;

    protected CoffeeMenu() {
    }

    public CoffeeMenu(String name, Long price) {
        this.name = name;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price;
    }
}