package com.example.coffeeorder.point.service;

import com.example.coffeeorder.point.entity.PointAccount;
import com.example.coffeeorder.point.entity.PointHistory;
import com.example.coffeeorder.point.repository.PointAccountRepository;
import com.example.coffeeorder.point.repository.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PointServiceIntegrationTest {

    @Autowired
    PointService pointService;

    @Autowired
    PointAccountRepository pointAccountRepository;

    @Autowired
    PointHistoryRepository pointHistoryRepository;

    @Test
    @Transactional
    void 포인트를_충전하면_실제_DB에_계정과_충전_이력이_저장된다() {
        PointAccount account =
                pointService.charge("user-1001", 5000L);

        PointAccount savedAccount =
                pointAccountRepository.findByUserId("user-1001")
                        .orElseThrow();

        System.out.println(
                "일반 충전 후 실제 잔액 = " + savedAccount.getBalance()
        );

        assertThat(savedAccount.getBalance())
                .as("충전 후 계정 잔액")
                .isEqualTo(5000L);

        PointHistory savedHistory =
                pointHistoryRepository.findAllByAccount_UserId("user-1001")
                        .get(0);

        System.out.println(
                "실제 history type = " + savedHistory.getType()
        );
        System.out.println(
                "실제 history amount = " + savedHistory.getAmount()
        );
        System.out.println(
                "실제 history balanceAfter = " + savedHistory.getBalanceAfter()
        );

        assertThat(savedHistory.getType().name())
                .as("충전 이력 타입")
                .isEqualTo("CHARGE");

        assertThat(savedHistory.getAmount())
                .as("충전 이력 금액")
                .isEqualTo(5000L);

        assertThat(savedHistory.getBalanceAfter())
                .as("충전 후 잔액 이력")
                .isEqualTo(5000L);
    }

    @Test
    void 동시에_포인트를_충전해도_잔액이_정확하다() throws InterruptedException {
        int numberOfRequests = 10;
        long chargeAmount = 1_000L;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfRequests);

        for (int i = 0; i < numberOfRequests; i++) {
            executorService.submit(() ->
                    pointService.charge("user-concurrent", chargeAmount)
            );
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        PointAccount account =
                pointAccountRepository.findByUserId("user-concurrent")
                        .orElseThrow();

        System.out.println(
                "동시 충전 후 실제 잔액 = " + account.getBalance()
        );

        assertThat(account.getBalance())
                .as("동시 충전 후 최종 잔액")
                .isEqualTo(numberOfRequests * chargeAmount);
    }
}