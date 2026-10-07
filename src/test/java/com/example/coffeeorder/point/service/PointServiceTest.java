package com.example.coffeeorder.point.service;

import com.example.coffeeorder.point.entity.PointAccount;
import com.example.coffeeorder.point.entity.PointHistory;
import com.example.coffeeorder.point.repository.PointAccountRepository;
import com.example.coffeeorder.point.repository.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    PointAccountRepository pointAccountRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    PointService pointService;

    @Test
    void 포인트를_충전할_수_있다() {
        // given
        PointAccount account = new PointAccount("user-1001");

        given(pointAccountRepository.findByUserId("user-1001"))
                .willReturn(Optional.of(account));

        // when
        PointAccount result = pointService.charge("user-1001", 5000L);

        // then
        assertThat(result.getUserId()).isEqualTo("user-1001");
        assertThat(result.getBalance()).isEqualTo(5000L);

        then(pointHistoryRepository)
                .should()
                .save(org.mockito.ArgumentMatchers.any(PointHistory.class));
    }
}