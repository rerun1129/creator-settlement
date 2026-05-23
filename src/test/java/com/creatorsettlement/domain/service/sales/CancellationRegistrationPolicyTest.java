package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
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

@DisplayName("CancellationRegistrationPolicy 단위 테스트")
class CancellationRegistrationPolicyTest {

    private InMemorySalesRepository salesRepository;
    private CancellationRegistrationPolicy policy;

    @BeforeEach
    void setUp() {
        salesRepository = new InMemorySalesRepository(new InMemoryCourseRepository());
        policy = new CancellationRegistrationPolicy(salesRepository);
    }

    @Test
    @DisplayName("존재하지 않는 SalesRecordId로 환불 등록 검증 시 예외가 발생한다")
    void validate_throwsException_whenSalesRecordNotFound() {
        // Given: 빈 salesRepository

        // When & Then
        assertThatThrownBy(() -> policy.validate(SalesRecordId.of(999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message());
    }

    @Test
    @DisplayName("존재하는 SalesRecordId로 검증 시 예외가 발생하지 않는다")
    void validate_doesNotThrow_whenSalesRecordExists() {
        // Given
        SalesRecord salesRecord = SalesRecord.of(
                CourseId.of(1L),
                StudentId.of(2L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0, 0))
        );
        salesRepository.saveSalesRecord(salesRecord);
        SalesRecordId savedId = SalesRecordId.of(1L);

        // When & Then
        assertThatNoException().isThrownBy(() -> policy.validate(savedId));
    }
}
