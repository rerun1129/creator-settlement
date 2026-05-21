package com.creatorsettlement.infrastructure.persistence.sale;

import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_record")
class SalesRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJpaEntity course;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    protected SalesRecordJpaEntity() {
    }

    static SalesRecordJpaEntity of(CourseJpaEntity course, Long studentId, BigDecimal paymentAmount, LocalDateTime paidAt) {
        SalesRecordJpaEntity entity = new SalesRecordJpaEntity();
        entity.course = course;
        entity.studentId = studentId;
        entity.paymentAmount = paymentAmount;
        entity.paidAt = paidAt;
        return entity;
    }

    Long getId() {
        return id;
    }

    CourseJpaEntity getCourse() {
        return course;
    }

    Long getStudentId() {
        return studentId;
    }

    BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    LocalDateTime getPaidAt() {
        return paidAt;
    }
}
