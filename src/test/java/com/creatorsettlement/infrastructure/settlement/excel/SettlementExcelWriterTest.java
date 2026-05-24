package com.creatorsettlement.infrastructure.settlement.excel;

import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.SettlementExcelDownload;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SettlementExcelWriterTest {

    private final SettlementExcelWriter writer = new SettlementExcelWriter();

    @Test
    @DisplayName("1행에 영문 헤더 creatorId와 expectedSettlementAmount를 작성한다")
    void writes_header_row_with_english_column_names() throws IOException {
        SettlementRangeView view = new SettlementRangeView(List.of(), BigDecimal.ZERO);

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("creatorId");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("expectedSettlementAmount");
        }
    }

    @Test
    @DisplayName("데이터 행을 creatorId 오름차순으로 정렬해서 작성한다")
    void writes_data_rows_sorted_by_creator_id_ascending() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(
                        new CreatorPayableView(3L, new BigDecimal("300")),
                        new CreatorPayableView(1L, new BigDecimal("100")),
                        new CreatorPayableView(2L, new BigDecimal("200"))
                ),
                new BigDecimal("600")
        );

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(100.0);
            assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(200.0);
            assertThat(sheet.getRow(3).getCell(0).getNumericCellValue()).isEqualTo(3.0);
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(300.0);
        }
    }

    @Test
    @DisplayName("데이터 행과 합계 행 사이에 빈 행 1행을 둔다")
    void writes_empty_row_between_data_and_total() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("100"))),
                new BigDecimal("100")
        );

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            Object emptyRow = sheet.getRow(2);
            assertThat(emptyRow).isNull();
        }
    }

    @Test
    @DisplayName("합계 행 좌측 셀에 '총합' 라벨과 우측 셀에 totalAmount를 작성한다")
    void writes_total_row_with_label_and_amount() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("100"))),
                new BigDecimal("64000")
        );

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("총합");
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(64000.0);
        }
    }

    @Test
    @DisplayName("응답이 비어도 헤더와 합계 0원 행을 작성한다")
    void writes_header_and_total_when_responses_empty() throws IOException {
        SettlementRangeView view = new SettlementRangeView(List.of(), BigDecimal.ZERO);

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("creatorId");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("expectedSettlementAmount");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("총합");
            assertThat(sheet.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("출력 byte 배열은 XSSFWorkbook으로 다시 파싱할 수 있다")
    void produces_parseable_xlsx_bytes() {
        SettlementRangeView view = new SettlementRangeView(List.of(), BigDecimal.ZERO);

        SettlementExcelDownload download = writer.write(view, java.time.LocalDate.of(2026, 5, 1), java.time.LocalDate.of(2026, 5, 31));
        byte[] bytes = download.body();

        assertThatCode(() -> {
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                workbook.getNumberOfSheets();
            }
        }).doesNotThrowAnyException();
    }
}
