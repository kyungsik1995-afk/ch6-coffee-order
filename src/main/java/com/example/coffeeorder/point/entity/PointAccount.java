package com.example.coffeeorder.point.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "point_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_account_user_id", columnNames = "user_id")
        }
)
public class PointAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false)
    private Long balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PointAccount() {
    }

    public PointAccount(String userId) {
        this.userId = userId;
        this.balance = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Long getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void charge(Long amount) {
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
    }
}