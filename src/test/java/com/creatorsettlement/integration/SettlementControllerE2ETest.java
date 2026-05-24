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
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@DisplayName("SettlementController E2E")
class SettlementControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private SalesRepository salesRepo;

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

    // ─── POST /api/settlements/confirm ─────────────────────────────────────────

    @Test
    @DisplayName("미저장 월 confirm 호출 시 sales 집계 산출 + 201 응답, 후속 GET CONFIRMED 확인")
    void confirm_returns_201_andCONFIRMED_when_not_stored() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 A");
        long courseId = saveCourse(creatorId, "강의 A");
        saveSale(courseId, 10L, "50000", LocalDateTime.of(2026, 5, 10, 12, 0));

        String body = """
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
                """.formatted(creatorId);

        // when
        mockMvc.perform(post("/api/settlements/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // then
        mockMvc.perform(get("/api/settlements")
                        .param("creatorId", String.valueOf(creatorId))
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").value("2026-06-01T10:00:00"))
                .andExpect(jsonPath("$.totalSales").value(50000));
    }

    @Test
    @DisplayName("이미 확정된 정산을 다시 confirm 호출 시 409 SETTLEMENT_ALREADY_CONFIRMED")
    void confirm_returns_409_ALREADY_CONFIRMED_when_already_confirmed() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 B");
        saveCourse(creatorId, "강의 B");
        String body = """
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
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
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
                """.formatted(creatorId);
        String payBody = """
                {"creatorId":%d,"yearMonth":"2026-05","paidAt":"2026-06-02T10:00:00"}
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
                {"creatorId":1,"yearMonth":"2026-05"}
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
                {"creatorId":1,"yearMonth":"2026/05","confirmedAt":"2026-06-01T10:00:00"}
                """;

        // when & then
        mockMvc.perform(post("/api/settlements/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── POST /api/settlements/pay ─────────────────────────────────────────────

    @Test
    @DisplayName("확정 정산 존재 시 pay 호출은 204 반환, 후속 GET PAID")
    void pay_returns_204_when_confirmed_exists() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 D");
        saveCourse(creatorId, "강의 D");
        String confirmBody = """
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
                """.formatted(creatorId);
        String payBody = """
                {"creatorId":%d,"yearMonth":"2026-05","paidAt":"2026-06-02T10:00:00"}
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
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("미저장 월 pay 호출 시 404 SETTLEMENT_NOT_FOUND")
    void pay_returns_404_when_not_stored() throws Exception {
        // given
        String body = """
                {"creatorId":999,"yearMonth":"2026-05","paidAt":"2026-06-02T10:00:00"}
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
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
                """.formatted(creatorId);
        String payBody = """
                {"creatorId":%d,"yearMonth":"2026-05","paidAt":"2026-06-02T10:00:00"}
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
                {"creatorId":1,"yearMonth":"2026-05"}
                """;

        // when & then
        mockMvc.perform(post("/api/settlements/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── 회귀: GET paidAt 응답 필드 ────────────────────────────────────────────

    @Test
    @DisplayName("PAID 상태 정산 GET 응답에 paidAt 노출")
    void get_returns_paidAt_when_settlement_is_paid() throws Exception {
        // given
        long creatorId = saveCreator("크리에이터 F");
        saveCourse(creatorId, "강의 F");
        String confirmBody = """
                {"creatorId":%d,"yearMonth":"2026-05","confirmedAt":"2026-06-01T10:00:00"}
                """.formatted(creatorId);
        String payBody = """
                {"creatorId":%d,"yearMonth":"2026-05","paidAt":"2026-06-02T10:00:00"}
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
                        .param("yearMonth", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAt").value("2026-06-02T10:00:00"));
    }

    // ─── GET /api/settlements/aggregate ────────────────────────────────────────

    @Test
    @DisplayName("기간 집계 조회 시 creatorId asc 정렬 + totalAmount 합산 정확")
    void aggregate_returns_200_with_sorted_payables_and_total() throws Exception {
        // given
        long creatorIdA = saveCreator("크리에이터 A");
        long creatorIdB = saveCreator("크리에이터 B");
        long creatorIdC = saveCreator("크리에이터 C");
        long courseA = saveCourse(creatorIdA, "강의 A");
        long courseB = saveCourse(creatorIdB, "강의 B");
        saveSale(courseA, 10L, "50000", LocalDateTime.of(2026, 5, 10, 12, 0));
        saveSale(courseB, 20L, "30000", LocalDateTime.of(2026, 5, 11, 12, 0));

        // when & then
        mockMvc.perform(get("/api/settlements/aggregate")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses.length()").value(3))
                .andExpect(jsonPath("$.responses[0].creatorId").value(creatorIdA))
                .andExpect(jsonPath("$.responses[0].expectedSettlementAmount").value(40000))
                .andExpect(jsonPath("$.responses[1].creatorId").value(creatorIdB))
                .andExpect(jsonPath("$.responses[1].expectedSettlementAmount").value(24000))
                .andExpect(jsonPath("$.responses[2].creatorId").value(creatorIdC))
                .andExpect(jsonPath("$.responses[2].expectedSettlementAmount").value(0))
                .andExpect(jsonPath("$.totalAmount").value(64000));
    }

    @Test
    @DisplayName("크리에이터 0명 조회 시 빈 배열과 0원 totalAmount 반환")
    void aggregate_returns_200_with_empty_responses_when_no_creators() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements/aggregate")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("from이 to보다 늦은 경우 400 VALIDATION")
    void aggregate_returns_400_VALIDATION_when_from_after_to() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements/aggregate")
                        .param("from", "2026-05-31")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    @DisplayName("from 날짜 포맷 오류 시 400 응답")
    void aggregate_returns_400_when_from_date_format_invalid() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements/aggregate")
                        .param("from", "invalid-date")
                        .param("to", "2026-05-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── GET /api/settlements/aggregate/download ─────────────────────────────

    @Test
    @DisplayName("다운로드 시 xlsx Content-Type과 attachment Content-Disposition 헤더를 반환한다")
    void download_returns_xlsx_with_correct_headers_and_filename() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements/aggregate/download")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"settlements_2026-05-01_2026-05-31.xlsx\""));
    }

    @Test
    @DisplayName("다운로드 본문에 creatorId 오름차순 데이터와 합계 행이 포함된다")
    void download_body_contains_sorted_data_and_total_row() throws Exception {
        // given
        long creatorIdA = saveCreator("크리에이터 A");
        long creatorIdB = saveCreator("크리에이터 B");
        long creatorIdC = saveCreator("크리에이터 C");
        long courseA = saveCourse(creatorIdA, "강의 A");
        long courseB = saveCourse(creatorIdB, "강의 B");
        saveSale(courseA, 10L, "50000", LocalDateTime.of(2026, 5, 10, 12, 0));
        saveSale(courseB, 20L, "30000", LocalDateTime.of(2026, 5, 11, 12, 0));

        // when
        byte[] body = mockMvc.perform(get("/api/settlements/aggregate/download")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // then
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFRow headerRow = sheet.getRow(0);
            org.junit.jupiter.api.Assertions.assertEquals("creatorId", headerRow.getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("expectedSettlementAmount", headerRow.getCell(1).getStringCellValue());

            org.junit.jupiter.api.Assertions.assertEquals((double) creatorIdA, sheet.getRow(1).getCell(0).getNumericCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(40000.0, sheet.getRow(1).getCell(1).getNumericCellValue());
            org.junit.jupiter.api.Assertions.assertEquals((double) creatorIdB, sheet.getRow(2).getCell(0).getNumericCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(24000.0, sheet.getRow(2).getCell(1).getNumericCellValue());
            org.junit.jupiter.api.Assertions.assertEquals((double) creatorIdC, sheet.getRow(3).getCell(0).getNumericCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(0.0, sheet.getRow(3).getCell(1).getNumericCellValue());

            org.junit.jupiter.api.Assertions.assertNull(sheet.getRow(4));

            XSSFRow total = sheet.getRow(5);
            org.junit.jupiter.api.Assertions.assertEquals("총합", total.getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(64000.0, total.getCell(1).getNumericCellValue());
        }
    }

    @Test
    @DisplayName("다운로드 요청에서 from이 to보다 늦으면 400 VALIDATION 응답")
    void download_returns_400_VALIDATION_when_from_after_to() throws Exception {
        // when & then
        mockMvc.perform(get("/api/settlements/aggregate/download")
                        .param("from", "2026-05-31")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    // ─── Fixture helpers ────────────────────────────────────────────────────────

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
