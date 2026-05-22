package com.creatorsettlement.presentation.dto;

import com.creatorsettlement.application.SalesListItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record SalesItemResponse(
        Long saleId,
        Long courseId,
        Long studentId,
        Long creatorId,
        BigDecimal paymentAmount,
        LocalDateTime paidAt,
        List<CancellationResponse> cancellations
) {
    public static SalesItemResponse from(SalesListItem item) {
        return new SalesItemResponse(
                item.saleId().value(),
                item.courseId().value(),
                item.studentId().value(),
                item.creatorId().value(),
                item.paymentAmount().value(),
                item.paidAt().value(),
                item.cancellations().stream().map(CancellationResponse::from).toList()
        );
    }

    public static List<SalesItemResponse> fromAllSortedByPaidAtDesc(List<SalesListItem> items) {
        return items.stream()
                .map(SalesItemResponse::from)
                .sorted(Comparator.comparing(SalesItemResponse::paidAt).reversed())
                .toList();
    }
}
