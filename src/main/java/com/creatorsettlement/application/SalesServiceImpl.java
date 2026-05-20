package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.SalesRepository;
import org.springframework.stereotype.Service;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;

    public SalesServiceImpl(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    @Override
    public void register(RegisterSaleCommand command) {
        CourseId courseId = CourseId.of(command.courseId());
        StudentId studentId = StudentId.of(command.studentId());
        Money paymentAmount = Money.of(command.paymentAmount());
        OccurredAt paidAt = OccurredAt.of(command.paidAt());
        SalesRecord salesRecord = new SalesRecord(courseId, studentId, paymentAmount, paidAt);
        salesRepository.save(salesRecord);
    }
}
