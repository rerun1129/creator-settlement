package com.creatorsettlement.integration;

import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import com.creatorsettlement.infrastructure.persistence.creator.CreatorJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("jpa-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SettlementController 확정·지급 E2E")
class SettlementLifecycleControllerE2ETest {

    @Nested
    @DisplayName("확정")
    class ConfirmEndpoint {

        @Autowired
        private MockMvc mockMvc;

        @PersistenceContext
        private EntityManager em;

        @Autowired
        private SalesRepository salesRepo;

        @Test
        @DisplayName("미저장 월 confirm 호출 시 sales 집계 산출 + 201 응답, 후속 GET CONFIRMED 확인")
        void confirm_returns_201_andCONFIRMED_when_not_stored() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 A");
            long courseId = saveCourse(creatorId, "강의 A");
            saveSale(courseId, 10L, "50000", LocalDateTime.of(2020, 1, 10, 12, 0));

            String body = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);

            // when
            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            // then
            mockMvc.perform(get("/api/settlements")
                            .param("creatorId", String.valueOf(creatorId))
                            .param("yearMonth", "2020-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.confirmedAt").value("2020-02-01T10:00:00"))
                    .andExpect(jsonPath("$.totalSales").value(50000));
        }

        @Test
        @DisplayName("이미 확정된 정산을 다시 confirm 호출 시 409 SETTLEMENT_ALREADY_CONFIRMED")
        void confirm_returns_409_ALREADY_CONFIRMED_when_already_confirmed() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 B");
            saveCourse(creatorId, "강의 B");
            String body = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            // when & then
            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_ALREADY_CONFIRMED"));
        }

        @Test
        @DisplayName("이미 지급된 정산을 confirm 호출 시 409 SETTLEMENT_ALREADY_PAID")
        void confirm_returns_409_ALREADY_PAID_when_already_paid() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 C");
            saveCourse(creatorId, "강의 C");
            String confirmBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);
            String payBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","paidAt":"2020-02-02T10:00:00"}
                    """.formatted(creatorId);

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payBody))
                    .andExpect(status().isNoContent());

            // when & then
            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_ALREADY_PAID"));
        }

        @Test
        @DisplayName("confirmedAt 누락 시 400 VALIDATION")
        void confirm_returns_400_VALIDATION_when_confirmedAt_missing() throws Exception {
            // given
            String body = """
                    {"creatorId":1,"yearMonth":"2020-01"}
                    """;

            // when & then
            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }

        @Test
        @DisplayName("yearMonth 형식 오류 시 400 VALIDATION")
        void confirm_returns_400_VALIDATION_when_yearMonth_format_invalid() throws Exception {
            // given
            String body = """
                    {"creatorId":1,"yearMonth":"2020/01","confirmedAt":"2020-02-01T10:00:00"}
                    """;

            // when & then
            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }

        @Test
        @DisplayName("현재 KST 월 confirm 호출 시 409 SETTLEMENT_MONTH_IN_PROGRESS")
        void confirm_returns_409_when_target_month_is_current_kst_month() throws Exception {
            long creatorId = saveCreator("크리에이터 가드 현재");
            saveCourse(creatorId, "강의 가드 현재");
            YearMonth current = YearMonth.now(ZoneId.of("Asia/Seoul"));
            String body = """
                    {"creatorId":%d,"yearMonth":"%s","confirmedAt":"2026-01-01T10:00:00"}
                    """.formatted(creatorId, current.toString());

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_MONTH_IN_PROGRESS"));
        }

        @Test
        @DisplayName("미래 월 confirm 호출 시 409 SETTLEMENT_MONTH_IN_PROGRESS")
        void confirm_returns_409_when_target_month_is_future() throws Exception {
            long creatorId = saveCreator("크리에이터 가드 미래");
            saveCourse(creatorId, "강의 가드 미래");
            YearMonth future = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
            String body = """
                    {"creatorId":%d,"yearMonth":"%s","confirmedAt":"2026-01-01T10:00:00"}
                    """.formatted(creatorId, future.toString());

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_MONTH_IN_PROGRESS"));
        }

        private long saveCreator(String name) {
            CreatorJpaEntity entity = CreatorJpaEntity.of(name);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }

        private long saveCourse(long creatorId, String title) {
            CourseJpaEntity entity = CourseJpaEntity.of(creatorId, title);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }

        private void saveSale(long courseId, long studentId, String paymentAmount, LocalDateTime paidAt) {
            salesRepo.saveSalesRecord(SalesRecord.of(
                    CourseId.of(courseId),
                    StudentId.of(studentId),
                    Money.of(new BigDecimal(paymentAmount)),
                    OccurredAt.of(paidAt)
            ));
        }
    }

    @Nested
    @DisplayName("지급")
    class PayEndpoint {

        @Autowired
        private MockMvc mockMvc;

        @PersistenceContext
        private EntityManager em;

        @Autowired
        private SalesRepository salesRepo;

        @Test
        @DisplayName("확정 정산 존재 시 pay 호출은 204 반환, 후속 GET PAID")
        void pay_returns_204_when_confirmed_exists() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 D");
            saveCourse(creatorId, "강의 D");
            String confirmBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);
            String payBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","paidAt":"2020-02-02T10:00:00"}
                    """.formatted(creatorId);

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isCreated());

            // when
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payBody))
                    .andExpect(status().isNoContent());

            // then
            mockMvc.perform(get("/api/settlements")
                            .param("creatorId", String.valueOf(creatorId))
                            .param("yearMonth", "2020-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("미저장 월 pay 호출 시 404 SETTLEMENT_NOT_FOUND")
        void pay_returns_404_when_not_stored() throws Exception {
            // given
            String body = """
                    {"creatorId":999,"yearMonth":"2020-01","paidAt":"2020-02-02T10:00:00"}
                    """;

            // when & then
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("이미 지급된 정산을 다시 pay 호출 시 409 SETTLEMENT_ALREADY_PAID")
        void pay_returns_409_ALREADY_PAID_when_already_paid() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 E");
            saveCourse(creatorId, "강의 E");
            String confirmBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);
            String payBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","paidAt":"2020-02-02T10:00:00"}
                    """.formatted(creatorId);

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payBody))
                    .andExpect(status().isNoContent());

            // when & then
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_ALREADY_PAID"));
        }

        @Test
        @DisplayName("paidAt 누락 시 400 VALIDATION")
        void pay_returns_400_VALIDATION_when_paidAt_missing() throws Exception {
            // given
            String body = """
                    {"creatorId":1,"yearMonth":"2020-01"}
                    """;

            // when & then
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }

        @Test
        @DisplayName("현재 KST 월 pay 호출 시 409 SETTLEMENT_MONTH_IN_PROGRESS")
        void pay_returns_409_when_target_month_is_current_kst_month() throws Exception {
            YearMonth current = YearMonth.now(ZoneId.of("Asia/Seoul"));
            String body = """
                    {"creatorId":1,"yearMonth":"%s","paidAt":"2026-01-01T10:00:00"}
                    """.formatted(current.toString());

            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_MONTH_IN_PROGRESS"));
        }

        @Test
        @DisplayName("미래 월 pay 호출 시 409 SETTLEMENT_MONTH_IN_PROGRESS")
        void pay_returns_409_when_target_month_is_future() throws Exception {
            YearMonth future = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
            String body = """
                    {"creatorId":1,"yearMonth":"%s","paidAt":"2026-01-01T10:00:00"}
                    """.formatted(future.toString());

            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SETTLEMENT_MONTH_IN_PROGRESS"));
        }

        private long saveCreator(String name) {
            CreatorJpaEntity entity = CreatorJpaEntity.of(name);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }

        private long saveCourse(long creatorId, String title) {
            CourseJpaEntity entity = CourseJpaEntity.of(creatorId, title);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }
    }

    @Nested
    @DisplayName("paidAt 회귀")
    class PaidAtRegression {

        @Autowired
        private MockMvc mockMvc;

        @PersistenceContext
        private EntityManager em;

        @Autowired
        private SalesRepository salesRepo;

        @Test
        @DisplayName("PAID 상태 정산 GET 응답에 paidAt 노출")
        void get_returns_paidAt_when_settlement_is_paid() throws Exception {
            // given
            long creatorId = saveCreator("크리에이터 F");
            saveCourse(creatorId, "강의 F");
            String confirmBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","confirmedAt":"2020-02-01T10:00:00"}
                    """.formatted(creatorId);
            String payBody = """
                    {"creatorId":%d,"yearMonth":"2020-01","paidAt":"2020-02-02T10:00:00"}
                    """.formatted(creatorId);

            mockMvc.perform(post("/api/settlements/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/settlements/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payBody))
                    .andExpect(status().isNoContent());

            // when & then
            mockMvc.perform(get("/api/settlements")
                            .param("creatorId", String.valueOf(creatorId))
                            .param("yearMonth", "2020-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paidAt").value("2020-02-02T10:00:00"));
        }

        private long saveCreator(String name) {
            CreatorJpaEntity entity = CreatorJpaEntity.of(name);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }

        private long saveCourse(long creatorId, String title) {
            CourseJpaEntity entity = CourseJpaEntity.of(creatorId, title);
            em.persist(entity);
            em.flush();
            return entity.getId();
        }
    }
}
