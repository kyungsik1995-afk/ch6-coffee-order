package com.example.coffeeorder.point.repository;

import com.example.coffeeorder.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    List<PointHistory> findAllByAccount_UserId(String userId);
}
