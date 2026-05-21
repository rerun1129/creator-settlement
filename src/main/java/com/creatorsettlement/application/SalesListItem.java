package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;

import java.util.List;

public record SalesListItem(
        SalesRecordId saleId,
        CourseId courseId,
        StudentId studentId,
        CreatorId creatorId,
        Money paymentAmount,
        OccurredAt paidAt,
        List<CancellationView> cancellations
) {}
