package com.example.coffeeorder.point.controller;

import com.example.coffeeorder.point.entity.PointAccount;
import com.example.coffeeorder.point.service.PointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PointService pointService;

    @Test
    void 음수_금액으로_충전할_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/points/charge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "userId": "user-1001",
                                    "amount": -1000
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POINT_001"))
                .andExpect(jsonPath("$.message")
                        .value("충전 요청이 올바르지 않습니다."));
    }

    @Test
    void 충전_금액은_0일_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/points/charge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "userId": "user-1001",
                                    "amount": 0
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POINT_001"))
                .andExpect(jsonPath("$.message")
                        .value("충전 요청이 올바르지 않습니다."));
    }

    @Test
    void userId는_비어_있을_수_없다() throws Exception {
        mockMvc.perform(
                        post("/api/points/charge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "userId": "",
                                    "amount": 5000
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POINT_001"))
                .andExpect(jsonPath("$.message")
                        .value("충전 요청이 올바르지 않습니다."));
    }

    @Test
    void 포인트를_충전할_수_있다() throws Exception {
        PointAccount account = new PointAccount("user-1001");
        account.charge(5000L);

        given(pointService.charge("user-1001", 5000L))
                .willReturn(account);

        mockMvc.perform(
                        post("/api/points/charge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "userId": "user-1001",
                                "amount": 5000
                            }
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1001"))
                .andExpect(jsonPath("$.chargedAmount").value(5000))
                .andExpect(jsonPath("$.balance").value(5000));
    }
}