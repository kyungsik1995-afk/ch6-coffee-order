package com.example.coffeeorder.point.repository;

import com.example.coffeeorder.point.entity.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO point_account
                (user_id, balance, created_at, updated_at)
            VALUES
                (:userId, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                user_id = user_id
            """, nativeQuery = true)
    void createIfNotExists(@Param("userId") String userId);

    Optional<PointAccount> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PointAccount p where p.userId = :userId")
    Optional<PointAccount> findByUserIdForUpdate(@Param("userId") String userId);
}