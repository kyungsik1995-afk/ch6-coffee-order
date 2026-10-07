package com.example.coffeeorder.point.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private PointAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointHistoryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected PointHistory() {
    }

    private PointHistory(
            PointAccount account,
            PointHistoryType type,
            Long amount,
            Long balanceAfter
    ) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
    }

    public static PointHistory charge(
            PointAccount account,
            Long amount,
            Long balanceAfter
    ) {
        return new PointHistory(
                account,
                PointHistoryType.CHARGE,
                amount,
                balanceAfter
        );
    }

    public Long getId() {
        return id;
    }

    public PointAccount getAccount() {
        return account;
    }

    public PointHistoryType getType() {
        return type;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}