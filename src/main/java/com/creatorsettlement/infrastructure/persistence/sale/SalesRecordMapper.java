package com.creatorsettlement.infrastructure.persistence.sale;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;

class SalesRecordMapper {

    private SalesRecordMapper() {
    }

    static SalesRecordJpaEntity toEntity(SalesRecord domain, CourseJpaEntity courseReference) {
        return SalesRecordJpaEntity.of(
                courseReference,
                domain.getStudentId().value(),
                domain.getPaymentAmount().value(),
                domain.getPaidAt().value()
        );
    }

    static SalesRecord toDomainSalesRecord(SalesRecordJpaEntity entity) {
        return SalesRecord.of(
                CourseId.of(entity.getCourse().getId()),
                StudentId.of(entity.getStudentId()),
                Money.of(entity.getPaymentAmount()),
                OccurredAt.of(entity.getPaidAt())
        );
    }

    static CancellationRecordJpaEntity toCancellationEntity(CancellationRecord domain) {
        return CancellationRecordJpaEntity.of(
                domain.getSalesRecordId().value(),
                domain.getRefundAmount().value(),
                domain.getCancelledAt().value()
        );
    }

    static CancellationRecord toDomainCancellation(CancellationRecordJpaEntity entity) {
        return CancellationRecord.of(
                SalesRecordId.of(entity.getSalesRecordId()),
                Money.of(entity.getRefundAmount()),
                OccurredAt.of(entity.getCancelledAt())
        );
    }
}
