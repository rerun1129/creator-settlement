package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
