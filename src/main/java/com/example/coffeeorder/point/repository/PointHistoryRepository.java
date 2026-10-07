package com.example.coffeeorder.point.repository;

import com.example.coffeeorder.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}