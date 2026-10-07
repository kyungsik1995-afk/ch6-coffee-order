package com.example.coffeeorder.menu.service;

import com.example.coffeeorder.menu.entity.CoffeeMenu;
import com.example.coffeeorder.menu.repository.CoffeeMenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoffeeMenuService {

    private final CoffeeMenuRepository coffeeMenuRepository;

    public CoffeeMenuService(CoffeeMenuRepository coffeeMenuRepository) {
        this.coffeeMenuRepository = coffeeMenuRepository;
    }

    public List<CoffeeMenu> getMenus() {
        return coffeeMenuRepository.findAll();
    }
}