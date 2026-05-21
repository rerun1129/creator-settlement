package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.SalesRecordView;

import java.util.List;

public record SalesListItem(
        SalesRecordId saleId,
        CourseId courseId,
        StudentId studentId,
        CreatorId creatorId,
        Money paymentAmount,
        OccurredAt paidAt,
        List<CancellationView> cancellations
) {
    public static SalesListItem from(SalesRecordView view) {
        List<CancellationView> cancellationViews = view.cancellations().stream()
                .map(CancellationView::from)
                .toList();
        return new SalesListItem(
                view.id(),
                view.record().getCourseId(),
                view.record().getStudentId(),
                view.creatorId(),
                view.record().getPaymentAmount(),
                view.record().getPaidAt(),
                cancellationViews
        );
    }
}
