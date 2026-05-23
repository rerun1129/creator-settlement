package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
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

@DisplayName("RefundPolicy 단위 테스트")
class RefundPolicyTest {

    private InMemorySalesRepository salesRepository;
    private RefundPolicy policy;

    @BeforeEach
    void setUp() {
        salesRepository = new InMemorySalesRepository(new InMemoryCourseRepository());
        policy = new RefundPolicy(salesRepository);
    }

    @Test
    @DisplayName("환불액이 결제 잔여액과 정확히 같으면 통과한다")
    void enforceRefundLimit_doesNotThrow_whenRefundEqualsRemaining() {
        // Given
        SalesRecord sale = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        salesRepository.saveSalesRecord(sale);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        // When & Then
        assertThatNoException().isThrownBy(() -> policy.enforceRefundLimit(sale, salesRecordId, Money.of(new BigDecimal("10000"))));
    }

    @Test
    @DisplayName("단일 환불액이 결제액을 초과하면 예외가 발생한다")
    void enforceRefundLimit_throwsException_whenSingleRefundExceedsPayment() {
        // Given
        SalesRecord sale = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        salesRepository.saveSalesRecord(sale);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);

        // When & Then
        assertThatThrownBy(() -> policy.enforceRefundLimit(sale, salesRecordId, Money.of(new BigDecimal("10001"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
    }

    @Test
    @DisplayName("기존 부분환불 후 잔여 한도 이내면 통과한다")
    void enforceRefundLimit_doesNotThrow_whenWithinRemainingAfterPriorRefund() {
        // Given
        SalesRecord sale = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        salesRepository.saveSalesRecord(sale);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                salesRecordId,
                Money.of(new BigDecimal("3000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 5, 10, 0, 0))
        ));

        // When & Then
        assertThatNoException().isThrownBy(() -> policy.enforceRefundLimit(sale, salesRecordId, Money.of(new BigDecimal("7000"))));
    }

    @Test
    @DisplayName("누적 환불 합계가 결제액을 초과하면 예외가 발생한다")
    void enforceRefundLimit_throwsException_whenSumExceedsPayment() {
        // Given
        SalesRecord sale = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        salesRepository.saveSalesRecord(sale);
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                salesRecordId,
                Money.of(new BigDecimal("3000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 5, 10, 0, 0))
        ));

        // When & Then
        assertThatThrownBy(() -> policy.enforceRefundLimit(sale, salesRecordId, Money.of(new BigDecimal("7001"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
    }
}
