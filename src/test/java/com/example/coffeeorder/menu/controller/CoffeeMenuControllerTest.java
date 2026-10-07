package com.example.coffeeorder.menu.controller;

import com.example.coffeeorder.menu.entity.CoffeeMenu;
import com.example.coffeeorder.menu.service.CoffeeMenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoffeeMenuController.class)
class CoffeeMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoffeeMenuService coffeeMenuService;

    @Test
    void 메뉴_목록을_조회할_수_있다() throws Exception {
        given(coffeeMenuService.getMenus())
                .willReturn(List.of(
                        new CoffeeMenu("아메리카노", 3000L),
                        new CoffeeMenu("카페라떼", 4000L)
                ));

        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("아메리카노"))
                .andExpect(jsonPath("$[0].price").value(3000))
                .andExpect(jsonPath("$[1].name").value("카페라떼"))
                .andExpect(jsonPath("$[1].price").value(4000));
    }
}