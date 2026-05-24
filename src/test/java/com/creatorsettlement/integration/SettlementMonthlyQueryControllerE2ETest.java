package com.creatorsettlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("jpa-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SettlementController 월별 조회 E2E")
class SettlementMonthlyQueryControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("미저장 월 조회 시 PENDING View와 0원 반환")
    void returns_pending_view_when_no_settlement_persisted() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements")
                        .param("creatorId", "1")
                        .param("yearMonth", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalSales").value(0))
                .andExpect(jsonPath("$.totalRefund").value(0))
                .andExpect(jsonPath("$.expectedPayout").value(0));
    }

    @Test
    @DisplayName("yearMonth 누락 시 400 VALIDATION 응답")
    void returns_400_when_year_month_missing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements")
                        .param("creatorId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
