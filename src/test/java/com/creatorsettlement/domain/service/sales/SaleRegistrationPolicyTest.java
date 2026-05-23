package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SaleRegistrationPolicy 단위 테스트")
class SaleRegistrationPolicyTest {

    private InMemoryCourseRepository courseRepository;
    private InMemorySalesRepository salesRepository;
    private SaleRegistrationPolicy policy;

    @BeforeEach
    void setUp() {
        courseRepository = new InMemoryCourseRepository();
        salesRepository = new InMemorySalesRepository(courseRepository);
        policy = new SaleRegistrationPolicy(courseRepository, salesRepository);
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 판매 등록 검증 시 예외가 발생한다")
    void validateRegistrable_throwsException_whenCourseNotFound() {
        // Given: 빈 courseRepository, 빈 salesRepository

        // When & Then
        assertThatThrownBy(() -> policy.validateRegistrable(CourseId.of(999L), StudentId.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.COURSE_NOT_FOUND_FOR_REGISTRATION.message());
    }

    @Test
    @DisplayName("동일 학생의 활성 판매가 존재하면 중복 등록을 차단한다")
    void validateRegistrable_throwsException_whenActiveSaleExists() {
        // Given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(2L);
        courseRepository.saveCourse(Course.of(courseId, CreatorId.of(100L), "샘플 강의"));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId,
                studentId,
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        ));

        // When & Then
        assertThatThrownBy(() -> policy.validateRegistrable(courseId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.DUPLICATE_ACTIVE_PURCHASE.message());
    }

    @Test
    @DisplayName("동일 학생의 부분환불 판매가 존재하면 중복 등록을 차단한다")
    void validateRegistrable_throwsException_whenPartiallyRefundedSaleExists() {
        // Given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(2L);
        courseRepository.saveCourse(Course.of(courseId, CreatorId.of(100L), "샘플 강의"));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId,
                studentId,
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        ));
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                salesRecordId,
                Money.of(new BigDecimal("3000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 5, 10, 0, 0))
        ));

        // When & Then
        assertThatThrownBy(() -> policy.validateRegistrable(courseId, studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.DUPLICATE_ACTIVE_PURCHASE.message());
    }

    @Test
    @DisplayName("동일 학생의 전액환불 판매만 존재하면 재등록을 허용한다")
    void validateRegistrable_doesNotThrow_whenFullyRefundedSaleExists() {
        // Given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(2L);
        courseRepository.saveCourse(Course.of(courseId, CreatorId.of(100L), "샘플 강의"));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId,
                studentId,
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        ));
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                salesRecordId,
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 5, 10, 0, 0))
        ));

        // When & Then
        assertThatNoException().isThrownBy(() -> policy.validateRegistrable(courseId, studentId));
    }

    @Test
    @DisplayName("판매 이력이 없으면 등록을 허용한다")
    void validateRegistrable_doesNotThrow_whenNoSaleHistory() {
        // Given
        CourseId courseId = CourseId.of(1L);
        courseRepository.saveCourse(Course.of(courseId, CreatorId.of(100L), "샘플 강의"));

        // When & Then
        assertThatNoException().isThrownBy(() -> policy.validateRegistrable(courseId, StudentId.of(2L)));
    }
}
