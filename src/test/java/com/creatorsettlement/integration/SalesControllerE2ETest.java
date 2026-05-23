package com.creatorsettlement.integration;

import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import com.creatorsettlement.infrastructure.persistence.creator.CreatorJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("jpa-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SalesController E2E")
class SalesControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private SalesRepository salesRepo;

    // ─── POST /api/sales ────────────────────────────────────────────────────────

    @Test
    @DisplayName("등록 대상 강의가 존재하면 201을 반환한다")
    void register_returns201_whenCourseExists() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 A");

        String body = """
                {"courseId":%d,"studentId":10,"paymentAmount":50000,"paidAt":"2026-05-01T12:00:00"}
                """.formatted(courseId);

        // when & then
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("등록 대상 강의가 없으면 400과 COURSE_NOT_FOUND_FOR_REGISTRATION을 반환한다")
    void register_returns400_whenCourseNotFound() throws Exception {
        // given
        String body = """
                {"courseId":1,"studentId":10,"paymentAmount":50000,"paidAt":"2026-05-01T12:00:00"}
                """;

        // when & then
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND_FOR_REGISTRATION"))
                .andExpect(jsonPath("$.message").value("등록 대상 강의를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("필수 paymentAmount가 누락되면 400과 VALIDATION을 반환한다")
    void register_returns400_whenPaymentAmountMissing() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 A");

        String body = """
                {"courseId":%d,"studentId":10,"paidAt":"2026-05-01T12:00:00"}
                """.formatted(courseId);

        // when & then
        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── POST /api/sales/cancellations ──────────────────────────────────────────

    @Test
    @DisplayName("원본 판매가 존재하고 환불 금액이 정상이면 201을 반환한다")
    void registerCancellation_returns201_whenOriginalSaleExists() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 B");
        saveSale(courseId, 10L, "50000", LocalDateTime.of(2026, 5, 1, 12, 0));
        long saleId = latestSaleId(creatorId, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 2, 0, 0));

        String body = objectMapper.writeValueAsString(Map.of(
                "salesRecordId", saleId,
                "refundAmount", 10000,
                "cancelledAt", "2026-05-02T12:00:00"
        ));

        // when & then
        mockMvc.perform(post("/api/sales/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("원본 판매가 없으면 400과 SALES_RECORD_NOT_FOUND를 반환한다")
    void registerCancellation_returns400_whenSaleNotFound() throws Exception {
        // given
        String body = """
                {"salesRecordId":999999,"refundAmount":10000,"cancelledAt":"2026-05-02T12:00:00"}
                """;

        // when & then
        mockMvc.perform(post("/api/sales/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SALES_RECORD_NOT_FOUND"));
    }

    @Test
    @DisplayName("환불 금액이 결제 금액을 초과하면 400과 REFUND_EXCEEDS_REMAINING을 반환한다")
    void registerCancellation_returns400_whenRefundExceedsRemaining() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 C");
        saveSale(courseId, 10L, "10000", LocalDateTime.of(2026, 5, 1, 12, 0));
        long saleId = latestSaleId(creatorId, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 2, 0, 0));

        String body = objectMapper.writeValueAsString(Map.of(
                "salesRecordId", saleId,
                "refundAmount", 20000,
                "cancelledAt", "2026-05-02T12:00:00"
        ));

        // when & then
        mockMvc.perform(post("/api/sales/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_REMAINING"));
    }

    @Test
    @DisplayName("동일 판매에 다회 환불 누적이 결제 금액을 초과하면 400과 REFUND_EXCEEDS_REMAINING을 반환한다")
    void registerCancellation_returns400_whenCumulativeRefundExceedsRemaining() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 G");
        saveSale(courseId, 10L, "10000", LocalDateTime.of(2026, 5, 1, 12, 0));
        long saleId = latestSaleId(creatorId, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 6, 1, 0, 0));

        String firstBody = objectMapper.writeValueAsString(Map.of(
                "salesRecordId", saleId,
                "refundAmount", 7000,
                "cancelledAt", "2026-05-02T12:00:00"
        ));
        String secondBody = objectMapper.writeValueAsString(Map.of(
                "salesRecordId", saleId,
                "refundAmount", 5000,
                "cancelledAt", "2026-05-03T12:00:00"
        ));

        // when & then
        mockMvc.perform(post("/api/sales/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/sales/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_REMAINING"));
    }

    // ─── GET /api/sales ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("데이터가 없으면 200과 빈 배열을 반환한다")
    void listSales_returns200WithEmptyArray_whenNoData() throws Exception {
        // when & then
        mockMvc.perform(get("/api/sales")
                        .param("from", "2026-04-01T00:00:00")
                        .param("toExclusive", "2026-05-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("여러 판매가 있으면 paidAt 내림차순으로 정렬된다")
    void listSales_returnsSortedByPaidAtDesc_whenMultipleSales() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 D");
        long courseId = saveCourse(creatorId, "강의 D");
        saveSale(courseId, 10L, "10000", LocalDateTime.of(2026, 4, 10, 10, 0));
        saveSale(courseId, 11L, "20000", LocalDateTime.of(2026, 4, 15, 10, 0));
        saveSale(courseId, 12L, "30000", LocalDateTime.of(2026, 4, 5, 10, 0));

        // when & then
        mockMvc.perform(get("/api/sales")
                        .param("from", "2026-04-01T00:00:00")
                        .param("toExclusive", "2026-05-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].paidAt").value("2026-04-15T10:00:00"))
                .andExpect(jsonPath("$[1].paidAt").value("2026-04-10T10:00:00"))
                .andExpect(jsonPath("$[2].paidAt").value("2026-04-05T10:00:00"));
    }

    @Test
    @DisplayName("creatorId를 지정하면 해당 크리에이터의 판매만 반환한다")
    void listSales_filtersByCreatorId_whenCreatorIdProvided() throws Exception {
        // given
        long creatorId100 = saveCreator("크리에이터 E");
        long creatorId200 = saveCreator("크리에이터 F");
        long courseId100 = saveCourse(creatorId100, "강의 E");
        long courseId200 = saveCourse(creatorId200, "강의 F");
        saveSale(courseId100, 10L, "10000", LocalDateTime.of(2026, 4, 10, 10, 0));
        saveSale(courseId200, 11L, "20000", LocalDateTime.of(2026, 4, 11, 10, 0));

        // when & then
        mockMvc.perform(get("/api/sales")
                        .param("creatorId", String.valueOf(creatorId100))
                        .param("from", "2026-04-01T00:00:00")
                        .param("toExclusive", "2026-05-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].creatorId").value(creatorId100));
    }

    @Test
    @DisplayName("필수 from 파라미터가 누락되면 400과 VALIDATION을 반환한다")
    void listSales_returns400_whenFromMissing() throws Exception {
        // when & then
        mockMvc.perform(get("/api/sales")
                        .param("toExclusive", "2026-05-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── Fixture helpers ─────────────────────────────────────────────────────────

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

    private long latestSaleId(long creatorId, LocalDateTime from, LocalDateTime toExclusive) {
        List<SalesRecordView> views = salesRepo.findSalesView(Optional.of(CreatorId.of(creatorId)), from, toExclusive);
        return views.get(0).id().value();
    }
}
