package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.course.CourseRepository;
import com.creatorsettlement.domain.repository.sales.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;

import java.util.List;

public class SaleRegistrationPolicy {

    private final CourseRepository courseRepository;
    private final SalesRepository salesRepository;

    public SaleRegistrationPolicy(CourseRepository courseRepository, SalesRepository salesRepository) {
        this.courseRepository = courseRepository;
        this.salesRepository = salesRepository;
    }

    public void validateRegistrable(CourseId courseId, StudentId studentId) {
        if (!courseRepository.existsByCourseId(courseId)) {
            throw new IllegalArgumentException(DomainErrorMessage.COURSE_NOT_FOUND_FOR_REGISTRATION.message());
        }
        if (hasActiveSale(courseId, studentId)) {
            throw new IllegalArgumentException(DomainErrorMessage.DUPLICATE_ACTIVE_PURCHASE.message());
        }
    }

    private boolean hasActiveSale(CourseId courseId, StudentId studentId) {
        List<SalesRecordWithId> sales = salesRepository.findByCourseIdAndStudentId(courseId, studentId);
        return sales.stream().anyMatch(sale -> !isFullyRefunded(sale));
    }

    private boolean isFullyRefunded(SalesRecordWithId sale) {
        Money refundedSum = salesRepository.sumRefundsBySalesRecordId(sale.id());
        return sale.record().getPaymentAmount().value().compareTo(refundedSum.value()) <= 0;
    }
}
