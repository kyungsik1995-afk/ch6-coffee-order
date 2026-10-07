package com.example.coffeeorder.point.service;

import com.example.coffeeorder.point.entity.PointAccount;
import com.example.coffeeorder.point.entity.PointHistory;
import com.example.coffeeorder.point.repository.PointAccountRepository;
import com.example.coffeeorder.point.repository.PointHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointService {

    private final PointAccountRepository pointAccountRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(
            PointAccountRepository pointAccountRepository,
            PointHistoryRepository pointHistoryRepository
    ) {
        this.pointAccountRepository = pointAccountRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    @Transactional
    public PointAccount charge(String userId, Long amount) {
        PointAccount account = pointAccountRepository.findByUserId(userId)
                .orElseGet(() -> createAccount(userId));

        account.charge(amount);

        PointHistory history = PointHistory.charge(
                account,
                amount,
                account.getBalance()
        );

        pointHistoryRepository.save(history);

        return account;
    }

    private PointAccount createAccount(String userId) {
        return pointAccountRepository.save(new PointAccount(userId));
    }
}