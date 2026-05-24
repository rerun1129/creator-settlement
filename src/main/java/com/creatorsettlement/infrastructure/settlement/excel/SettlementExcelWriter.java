package com.creatorsettlement.infrastructure.settlement.excel;

import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.SettlementExcelDownload;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class SettlementExcelWriter {

    private static final String SHEET_NAME = "settlements";
    private static final String HEADER_CREATOR_ID = "creatorId";
    private static final String HEADER_EXPECTED_SETTLEMENT_AMOUNT = "expectedSettlementAmount";
    private static final String TOTAL_LABEL = "총합";
    private static final String ACCOUNTING_FORMAT = "#,##0";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int CREATOR_ID_COLUMN = 0;
    private static final int AMOUNT_COLUMN = 1;
    private static final int[] ALL_COLUMNS = {CREATOR_ID_COLUMN, AMOUNT_COLUMN};
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String FILENAME_FORMAT = "settlements_%s_%s.xlsx";

    public SettlementExcelDownload write(SettlementRangeView view, LocalDate from, LocalDate to) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(SHEET_NAME);
            CellStyles styles = createStyles(workbook);
            writeHeader(sheet, styles.headerStyle);
            int nextRowIndex = writeDataRows(sheet, view.responses(), styles.bodyTextStyle, styles.bodyAmountStyle);
            int totalRowIndex = nextRowIndex + 1;
            writeTotalRow(sheet, totalRowIndex, view.totalAmount().doubleValue(),
                    styles.totalLabelStyle, styles.totalAmountStyle);
            autoSizeColumns(sheet);
            workbook.write(output);
            byte[] body = output.toByteArray();
            String filename = String.format(FILENAME_FORMAT, from, to);
            return SettlementExcelDownload.of(body, filename, XLSX_CONTENT_TYPE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CellStyles createStyles(XSSFWorkbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();
        short accountingFormat = dataFormat.getFormat(ACCOUNTING_FORMAT);

        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        applyThinBorder(headerStyle);
        headerStyle.setFont(boldFont);

        CellStyle bodyTextStyle = workbook.createCellStyle();
        applyThinBorder(bodyTextStyle);

        CellStyle bodyAmountStyle = workbook.createCellStyle();
        applyThinBorder(bodyAmountStyle);
        bodyAmountStyle.setDataFormat(accountingFormat);

        CellStyle totalLabelStyle = workbook.createCellStyle();
        applyThinBorder(totalLabelStyle);
        totalLabelStyle.setFont(boldFont);

        CellStyle totalAmountStyle = workbook.createCellStyle();
        applyThinBorder(totalAmountStyle);
        totalAmountStyle.setFont(boldFont);
        totalAmountStyle.setDataFormat(accountingFormat);

        return new CellStyles(headerStyle, bodyTextStyle, bodyAmountStyle, totalLabelStyle, totalAmountStyle);
    }

    private void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        short black = IndexedColors.BLACK.getIndex();
        style.setTopBorderColor(black);
        style.setBottomBorderColor(black);
        style.setLeftBorderColor(black);
        style.setRightBorderColor(black);
    }

    private void writeHeader(XSSFSheet sheet, CellStyle headerStyle) {
        XSSFRow header = sheet.createRow(HEADER_ROW_INDEX);
        XSSFCell creatorIdCell = header.createCell(CREATOR_ID_COLUMN);
        creatorIdCell.setCellValue(HEADER_CREATOR_ID);
        creatorIdCell.setCellStyle(headerStyle);
        XSSFCell amountCell = header.createCell(AMOUNT_COLUMN);
        amountCell.setCellValue(HEADER_EXPECTED_SETTLEMENT_AMOUNT);
        amountCell.setCellStyle(headerStyle);
    }

    private int writeDataRows(XSSFSheet sheet, List<CreatorPayableView> responses,
                              CellStyle bodyTextStyle, CellStyle bodyAmountStyle) {
        List<CreatorPayableView> sorted = responses.stream()
                .sorted(Comparator.comparing(CreatorPayableView::creatorId))
                .toList();
        int rowIndex = DATA_START_ROW_INDEX;
        for (CreatorPayableView view : sorted) {
            XSSFRow row = sheet.createRow(rowIndex);
            XSSFCell creatorIdCell = row.createCell(CREATOR_ID_COLUMN);
            creatorIdCell.setCellValue((double) view.creatorId());
            creatorIdCell.setCellStyle(bodyTextStyle);
            XSSFCell amountCell = row.createCell(AMOUNT_COLUMN);
            amountCell.setCellValue(view.expectedSettlementAmount().doubleValue());
            amountCell.setCellStyle(bodyAmountStyle);
            rowIndex++;
        }
        return rowIndex;
    }

    private void writeTotalRow(XSSFSheet sheet, int rowIndex, double totalAmount,
                               CellStyle totalLabelStyle, CellStyle totalAmountStyle) {
        XSSFRow row = sheet.createRow(rowIndex);
        XSSFCell labelCell = row.createCell(CREATOR_ID_COLUMN);
        labelCell.setCellValue(TOTAL_LABEL);
        labelCell.setCellStyle(totalLabelStyle);
        XSSFCell amountCell = row.createCell(AMOUNT_COLUMN);
        amountCell.setCellValue(totalAmount);
        amountCell.setCellStyle(totalAmountStyle);
    }

    private void autoSizeColumns(XSSFSheet sheet) {
        for (int column : ALL_COLUMNS) {
            sheet.autoSizeColumn(column);
        }
    }

    private record CellStyles(CellStyle headerStyle,
                              CellStyle bodyTextStyle,
                              CellStyle bodyAmountStyle,
                              CellStyle totalLabelStyle,
                              CellStyle totalAmountStyle) {
    }
}
