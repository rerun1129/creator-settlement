package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
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

class SalesServiceTest {

    private InMemorySalesRepository repository;
    private SalesService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySalesRepository();
        service = new SalesServiceImpl(repository);
    }

    @Test
    @DisplayName("판매 등록 시 입력값이 SalesRecord로 변환되어 저장된다")
    void register_persistsSalesRecord_withGivenCommand() {
        // Given
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
    @DisplayName("판매 등록 1회는 저장소에 정확히 1건만 추가한다")
    void register_appendsExactlyOneRecord_perInvocation() {
        // Given
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
        RegisterSaleCommand command = new RegisterSaleCommand(1L, 2L, new BigDecimal("10000"), paidAt);

        // When
        service.register(command);

        // Then
        assertThat(repository.findAll()).hasSize(1);
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
        Money cumulative = repository.sumRefundsBySalesRecordId(salesRecordId);
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
}
