package com.creatorsettlement.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("jpa-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("FeePolicyController E2E")
class FeePolicyControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("미래 effectiveFrom으로 POST 시 201 Created + Location 헤더 반환")
    void register_returns201_whenFutureEffectiveFrom() throws Exception {
        // given
        String body = """
                {"rate":0.1,"effectiveFrom":"2099-01-01"}
                """;

        // when & then
        mockMvc.perform(post("/api/fee-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/fee-policies"));
    }

    @Test
    @DisplayName("POST 등록 후 GET 호출 시 등록한 정책 노출")
    void listAll_returnsRegisteredPolicy_afterPost() throws Exception {
        // given
        String body = """
                {"rate":0.15,"effectiveFrom":"2099-02-01"}
                """;
        mockMvc.perform(post("/api/fee-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(get("/api/fee-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rate").value(0.15))
                .andExpect(jsonPath("$[0].effectiveFrom").value("2099-02-01"));
    }

    @Test
    @DisplayName("과거 effectiveFrom으로 POST 시 400 VALIDATION 응답 (@Future 위반)")
    void register_returns400_whenPastEffectiveFrom() throws Exception {
        // given
        String body = """
                {"rate":0.1,"effectiveFrom":"2000-01-01"}
                """;

        // when & then
        mockMvc.perform(post("/api/fee-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("동일 effectiveFrom 중복 등록 시 409 FEE_POLICY_DUPLICATE_EFFECTIVE_FROM 응답")
    void register_returns409_whenDuplicateEffectiveFrom() throws Exception {
        // given
        String body = """
                {"rate":0.1,"effectiveFrom":"2099-03-01"}
                """;
        mockMvc.perform(post("/api/fee-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(post("/api/fee-policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEE_POLICY_DUPLICATE_EFFECTIVE_FROM"));
    }
}
