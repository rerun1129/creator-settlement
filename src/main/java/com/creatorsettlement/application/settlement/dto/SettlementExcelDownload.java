package com.creatorsettlement.application.settlement.dto;

public record SettlementExcelDownload(
        byte[] body,
        String filename,
        String contentType,
        String contentDisposition
) {
    public static SettlementExcelDownload of(byte[] body, String filename, String contentType) {
        String contentDisposition = "attachment; filename=\"" + filename + "\"";
        return new SettlementExcelDownload(body, filename, contentType, contentDisposition);
    }
}
