package com.example.coffeeorder.menu.repository;

import com.example.coffeeorder.menu.entity.CoffeeMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeMenuRepository extends JpaRepository<CoffeeMenu, Long> {
}