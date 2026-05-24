package com.creatorsettlement.infrastructure.settlement.excel;

import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.SettlementExcelDownload;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementExcelWriterStyleTest {

    private final SettlementExcelWriter writer = new SettlementExcelWriter();

    @Test
    @DisplayName("데이터 행 금액 셀에 회계 포맷 #,##0을 적용한다")
    void applies_accounting_format_to_data_amount_cell() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("1000"))),
                new BigDecimal("1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            XSSFCell amountCell = sheet.getRow(1).getCell(1);
            assertThat(amountCell.getCellStyle().getDataFormatString()).isEqualTo("#,##0");
        }
    }

    @Test
    @DisplayName("합계 행 금액 셀에 회계 포맷 #,##0을 적용한다")
    void applies_accounting_format_to_total_amount_cell() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("1000"))),
                new BigDecimal("1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            XSSFCell totalAmountCell = sheet.getRow(3).getCell(1);
            assertThat(totalAmountCell.getCellStyle().getDataFormatString()).isEqualTo("#,##0");
        }
    }

    @Test
    @DisplayName("식별자 컬럼(creatorId)에는 회계 포맷을 적용하지 않는다")
    void does_not_apply_accounting_format_to_creator_id_cell() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("1000"))),
                new BigDecimal("1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            XSSFCell creatorIdCell = sheet.getRow(1).getCell(0);
            assertThat(creatorIdCell.getCellStyle().getDataFormatString()).isNotEqualTo("#,##0");
        }
    }

    @Test
    @DisplayName("헤더 행 셀에 BOLD 폰트를 적용한다")
    void applies_bold_font_to_header_cells() throws IOException {
        SettlementRangeView view = new SettlementRangeView(List.of(), BigDecimal.ZERO);

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            XSSFRow header = sheet.getRow(0);
            assertThat(workbook.getFontAt(header.getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(workbook.getFontAt(header.getCell(1).getCellStyle().getFontIndex()).getBold()).isTrue();
        }
    }

    @Test
    @DisplayName("헤더 행 셀에 4방향 THIN 외곽선을 적용한다")
    void applies_thin_border_on_all_sides_to_header_cells() throws IOException {
        SettlementRangeView view = new SettlementRangeView(List.of(), BigDecimal.ZERO);

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertCellHasThinBorder(sheet.getRow(0).getCell(0));
            assertCellHasThinBorder(sheet.getRow(0).getCell(1));
        }
    }

    @Test
    @DisplayName("데이터 행 셀에 4방향 THIN 외곽선을 적용한다")
    void applies_thin_border_on_all_sides_to_data_cells() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("1000"))),
                new BigDecimal("1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertCellHasThinBorder(sheet.getRow(1).getCell(0));
            assertCellHasThinBorder(sheet.getRow(1).getCell(1));
        }
    }

    @Test
    @DisplayName("합계 행 셀에 4방향 THIN 외곽선을 적용한다")
    void applies_thin_border_on_all_sides_to_total_cells() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("1000"))),
                new BigDecimal("1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            assertCellHasThinBorder(sheet.getRow(3).getCell(0));
            assertCellHasThinBorder(sheet.getRow(3).getCell(1));
        }
    }

    @Test
    @DisplayName("음수 금액도 회계 포맷으로 -1,000 표시 규칙을 위해 같은 #,##0 포맷을 보유한다")
    void negative_amount_cell_keeps_same_accounting_format() throws IOException {
        SettlementRangeView view = new SettlementRangeView(
                List.of(new CreatorPayableView(1L, new BigDecimal("-1000"))),
                new BigDecimal("-1000")
        );

        SettlementExcelDownload download = writer.write(view, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download.body()))) {
            XSSFSheet sheet = workbook.getSheet("settlements");
            XSSFCell amountCell = sheet.getRow(1).getCell(1);
            assertThat(amountCell.getNumericCellValue()).isEqualTo(-1000.0);
            assertThat(amountCell.getCellStyle().getDataFormatString()).isEqualTo("#,##0");
        }
    }

    private void assertCellHasThinBorder(XSSFCell cell) {
        CellStyle style = cell.getCellStyle();
        assertThat(style.getBorderTop()).isEqualTo(BorderStyle.THIN);
        assertThat(style.getBorderBottom()).isEqualTo(BorderStyle.THIN);
        assertThat(style.getBorderLeft()).isEqualTo(BorderStyle.THIN);
        assertThat(style.getBorderRight()).isEqualTo(BorderStyle.THIN);
    }
}
