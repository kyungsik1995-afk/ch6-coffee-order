package com.example.coffeeorder.point.repository;

import com.example.coffeeorder.point.entity.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PointAccount> findByUserId(String userId);
}