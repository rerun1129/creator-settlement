package com.creatorsettlement.infrastructure.settlement.excel;

import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;

@Component
public class SettlementExcelWriter {

    private static final String SHEET_NAME = "settlements";
    private static final String HEADER_CREATOR_ID = "creatorId";
    private static final String HEADER_EXPECTED_SETTLEMENT_AMOUNT = "expectedSettlementAmount";
    private static final String TOTAL_LABEL = "총합";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int CREATOR_ID_COLUMN = 0;
    private static final int AMOUNT_COLUMN = 1;

    public byte[] write(SettlementRangeView view) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(SHEET_NAME);
            writeHeader(sheet);
            int nextRowIndex = writeDataRows(sheet, view.responses());
            int totalRowIndex = nextRowIndex + 1;
            writeTotalRow(sheet, totalRowIndex, view.totalAmount().doubleValue());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeHeader(XSSFSheet sheet) {
        XSSFRow header = sheet.createRow(HEADER_ROW_INDEX);
        header.createCell(CREATOR_ID_COLUMN).setCellValue(HEADER_CREATOR_ID);
        header.createCell(AMOUNT_COLUMN).setCellValue(HEADER_EXPECTED_SETTLEMENT_AMOUNT);
    }

    private int writeDataRows(XSSFSheet sheet, List<CreatorPayableView> responses) {
        List<CreatorPayableView> sorted = responses.stream()
                .sorted(Comparator.comparing(CreatorPayableView::creatorId))
                .toList();
        int rowIndex = DATA_START_ROW_INDEX;
        for (CreatorPayableView view : sorted) {
            XSSFRow row = sheet.createRow(rowIndex);
            row.createCell(CREATOR_ID_COLUMN).setCellValue((double) view.creatorId());
            row.createCell(AMOUNT_COLUMN).setCellValue(view.expectedSettlementAmount().doubleValue());
            rowIndex++;
        }
        return rowIndex;
    }

    private void writeTotalRow(XSSFSheet sheet, int rowIndex, double totalAmount) {
        XSSFRow row = sheet.createRow(rowIndex);
        row.createCell(CREATOR_ID_COLUMN).setCellValue(TOTAL_LABEL);
        row.createCell(AMOUNT_COLUMN).setCellValue(totalAmount);
    }
}
