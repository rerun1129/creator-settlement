package com.creatorsettlement.application.sales;

import com.creatorsettlement.application.sales.dto.RegisterCancellationCommand;
import com.creatorsettlement.application.sales.dto.RegisterSaleCommand;
import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.sales.Cancellations;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.service.sales.CancellationRegistrationPolicy;
import com.creatorsettlement.domain.service.sales.RefundPolicy;
import com.creatorsettlement.domain.service.sales.SaleRegistrationPolicy;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SalesService 등록(register/registerCancellation) 단위 테스트")
class SalesServiceTest {

    private InMemorySalesRepository repository;
    private InMemoryCourseRepository courseRepository;
    private SalesService service;

    @BeforeEach
    void setUp() {
        courseRepository = new InMemoryCourseRepository();
        repository = new InMemorySalesRepository(courseRepository);
        RefundPolicy refundPolicy = new RefundPolicy(repository);
        SaleRegistrationPolicy registrationPolicy = new SaleRegistrationPolicy(courseRepository, repository);
        CancellationRegistrationPolicy cancellationRegistrationPolicy = new CancellationRegistrationPolicy(repository);
        service = new SalesServiceImpl(repository, refundPolicy, registrationPolicy, cancellationRegistrationPolicy);
    }

    @Test
    @DisplayName("판매 등록 시 입력값이 SalesRecord로 변환되어 저장된다")
    void register_persistsSalesRecord_withGivenCommand() {
        // Given
        courseRepository.saveCourse(Course.of(CourseId.of(1L), CreatorId.of(100L), "샘플 강의"));
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        RegisterSaleCommand command = new RegisterSaleCommand(1L, 2L, new BigDecimal("10000"), paidAt);

        // When
        service.register(command);

        // Then
        List<SalesRecord> saved = repository.findAll();
        assertThat(saved).hasSize(1);
        SalesRecord record = saved.get(0);
        assertThat(record.getCourseId().value()).isEqualTo(command.courseId());
        assertThat(record.getStudentId().value()).isEqualTo(command.studentId());
        assertThat(record.getPaymentAmount().value()).isEqualByComparingTo(command.paymentAmount());
        assertThat(record.getPaidAt().value()).isEqualTo(command.paidAt());
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 판매 등록 시 예외가 발생한다")
    void register_throwsException_whenCourseDoesNotExist() {
        // Given
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        RegisterSaleCommand command = new RegisterSaleCommand(
                999L,
                2L,
                new BigDecimal("10000"),
                paidAt
        );

        // When & Then
        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.COURSE_NOT_FOUND_FOR_REGISTRATION.message());
    }

    @Test
    @DisplayName("취소 등록 시 환불 금액이 누적 환불액으로 저장된다")
    void registerCancellation_persistsCancellationRecord_whenSalesRecordExists() {
        // Given
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        SalesRecord salesRecord = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(paidAt)
        );
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        LocalDateTime cancelledAt = LocalDateTime.of(2026, 5, 10, 12, 0, 0);
        RegisterCancellationCommand cancelCommand = new RegisterCancellationCommand(salesRecordId.value(), new BigDecimal("3000"), cancelledAt);

        // When
        service.registerCancellation(cancelCommand);

        // Then
        Money cumulative = Cancellations.of(repository.findCancellationsBySalesRecordId(salesRecordId)).total();
        assertThat(cumulative.value()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("존재하지 않는 SalesRecord ID로 취소 등록 시 예외가 발생한다")
    void registerCancellation_throwsException_whenSalesRecordNotFound() {
        // Given
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 5, 10, 12, 0, 0);
        RegisterCancellationCommand cancelCommand = new RegisterCancellationCommand(999L, new BigDecimal("3000"), cancelledAt);

        // When & Then
        assertThatThrownBy(() -> service.registerCancellation(cancelCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message());
    }

    @Test
    @DisplayName("환불 금액과 누적 환불액의 합이 결제 금액과 같으면 통과한다")
    void registerCancellation_persistsCancellationRecord_whenRefundEqualsRemaining() {
        // Given
        SalesRecord salesRecord = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        RegisterCancellationCommand cancelCommand = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("10000"), LocalDateTime.of(2026, 5, 10, 12, 0, 0));

        // When & Then
        assertThatNoException().isThrownBy(() -> service.registerCancellation(cancelCommand));
    }

    @Test
    @DisplayName("환불 금액이 결제 금액을 초과하면 예외가 발생한다")
    void registerCancellation_throwsException_whenRefundExceedsRemaining() {
        // Given
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        SalesRecord salesRecord = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(paidAt)
        );
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        LocalDateTime cancelledAt = LocalDateTime.of(2026, 5, 10, 12, 0, 0);
        RegisterCancellationCommand cancelCommand = new RegisterCancellationCommand(salesRecordId.value(), new BigDecimal("15000"), cancelledAt);

        // When & Then
        assertThatThrownBy(() -> service.registerCancellation(cancelCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
    }

    @Test
    @DisplayName("이전 환불이 있어도 잔여 범위 내 환불은 누적되어 저장된다")
    void registerCancellation_persistsCancellationRecord_whenWithinRemainingAfterPriorRefund() {
        // Given
        SalesRecord salesRecord = SalesRecord.of(CourseId.of(1L), StudentId.of(2L),
                Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0)));
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        RegisterCancellationCommand firstCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("3000"), LocalDateTime.of(2026, 5, 10, 12, 0, 0));
        service.registerCancellation(firstCancel);

        RegisterCancellationCommand secondCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("5000"), LocalDateTime.of(2026, 5, 15, 12, 0, 0));

        // When
        service.registerCancellation(secondCancel);

        // Then
        Money cumulative = Cancellations.of(repository.findCancellationsBySalesRecordId(salesRecordId)).total();
        assertThat(cumulative.value()).isEqualByComparingTo(new BigDecimal("8000"));
    }

    @Test
    @DisplayName("이전 환불과의 누적합이 결제 금액과 같으면 통과한다")
    void registerCancellation_persistsCancellationRecord_whenSumEqualsPaymentAcrossMultipleRefunds() {
        // Given
        SalesRecord salesRecord = SalesRecord.of(CourseId.of(1L), StudentId.of(2L),
                Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0)));
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        RegisterCancellationCommand firstCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("3000"), LocalDateTime.of(2026, 5, 10, 12, 0, 0));
        service.registerCancellation(firstCancel);

        RegisterCancellationCommand secondCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("7000"), LocalDateTime.of(2026, 5, 15, 12, 0, 0));

        // When
        service.registerCancellation(secondCancel);

        // Then
        Money cumulative = Cancellations.of(repository.findCancellationsBySalesRecordId(salesRecordId)).total();
        assertThat(cumulative.value()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("이전 환불과의 누적합이 결제 금액을 초과하면 예외가 발생한다")
    void registerCancellation_throwsException_whenSumExceedsPaymentAcrossMultipleRefunds() {
        // Given
        SalesRecord salesRecord = SalesRecord.of(CourseId.of(1L), StudentId.of(2L),
                Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0)));
        repository.saveSalesRecord(salesRecord);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        RegisterCancellationCommand firstCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("7000"), LocalDateTime.of(2026, 5, 10, 12, 0, 0));
        service.registerCancellation(firstCancel);

        RegisterCancellationCommand secondCancel = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("5000"), LocalDateTime.of(2026, 5, 15, 12, 0, 0));

        // When & Then
        assertThatThrownBy(() -> service.registerCancellation(secondCancel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
    }

    @Test
    @DisplayName("동일 학생의 동일 강의에 활성 결제가 있으면 중복 등록 시 예외가 발생한다")
    void register_throwsException_whenActiveSaleExists() {
        // Given
        courseRepository.saveCourse(Course.of(CourseId.of(1L), CreatorId.of(1L), "샘플 강의"));
        RegisterSaleCommand firstCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        service.register(firstCommand);

        RegisterSaleCommand duplicateCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 2, 10, 0, 0));

        // When & Then
        assertThatThrownBy(() -> service.register(duplicateCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.DUPLICATE_ACTIVE_PURCHASE.message());
    }

    @Test
    @DisplayName("동일 학생의 동일 강의가 전액 환불된 상태면 재등록이 성공한다")
    void register_succeeds_whenFullyRefundedSaleExists() {
        // Given
        courseRepository.saveCourse(Course.of(CourseId.of(1L), CreatorId.of(1L), "샘플 강의"));
        RegisterSaleCommand firstCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        service.register(firstCommand);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        RegisterCancellationCommand fullRefund = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("50000"), LocalDateTime.of(2026, 5, 3, 10, 0, 0));
        service.registerCancellation(fullRefund);

        RegisterSaleCommand reRegisterCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 5, 10, 0, 0));

        // When & Then
        assertThatNoException().isThrownBy(() -> service.register(reRegisterCommand));
    }

    @Test
    @DisplayName("동일 학생의 동일 강의가 부분 환불 상태(잔여 활성)면 중복 등록 시 예외가 발생한다")
    void register_throwsException_whenPartiallyRefundedSaleExists() {
        // Given
        courseRepository.saveCourse(Course.of(CourseId.of(1L), CreatorId.of(1L), "샘플 강의"));
        RegisterSaleCommand firstCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        service.register(firstCommand);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        RegisterCancellationCommand partialRefund = new RegisterCancellationCommand(
                salesRecordId.value(), new BigDecimal("30000"), LocalDateTime.of(2026, 5, 3, 10, 0, 0));
        service.registerCancellation(partialRefund);

        RegisterSaleCommand duplicateCommand = new RegisterSaleCommand(
                1L, 2L, new BigDecimal("50000"), LocalDateTime.of(2026, 5, 5, 10, 0, 0));

        // When & Then
        assertThatThrownBy(() -> service.register(duplicateCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.DUPLICATE_ACTIVE_PURCHASE.message());
    }
}
