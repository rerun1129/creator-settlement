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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("jpa-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("SettlementController 범위 집계 E2E")
class SettlementRangeControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private SalesRepository salesRepo;

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
