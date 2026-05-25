package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.settlement.SettlementStatus;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.service.fee.FeePolicyDomainService;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("미저장 월 정산 산출 도메인 서비스 단위 테스트")
class PendingSettlementResolverTest {

    private InMemorySalesRepository salesRepository;
    private InMemoryCourseRepository courseRepository;
    private InMemoryFeePolicyRepository feePolicyRepository;
    private FeePolicyDomainService feePolicyDomainService;
    private PendingSettlementResolver resolver;

    @BeforeEach
    void setUp() {
        courseRepository = new InMemoryCourseRepository();
        salesRepository = new InMemorySalesRepository(courseRepository);
        feePolicyRepository = new InMemoryFeePolicyRepository();
        feePolicyDomainService = new FeePolicyDomainService(feePolicyRepository);
        feePolicyRepository.save(FeePolicy.of(FeeRate.of(new BigDecimal("0.2")), LocalDate.of(2020, 1, 1)));
        resolver = new PendingSettlementResolver(salesRepository, feePolicyDomainService, new MonthlySettlementCalculator());
    }

    @Test
    @DisplayName("판매·취소 내역 없을 때 PENDING 상태 + totals 0 정산을 반환한다")
    void resolve_returns_PENDING_with_zero_totals_when_no_sales() {
        // Given
        CreatorId creatorId = CreatorId.of(80L);
        YearMonth yearMonth = YearMonth.of(2026, 4);

        // When
        Settlement result = resolver.resolve(creatorId, yearMonth);

        // Then
        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorId()).isEqualTo(creatorId);
        assertThat(result.yearMonth()).isEqualTo(yearMonth);
        assertThat(result.totalSales().value()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalRefund().value()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(result.salesCount()).isEqualTo(0L);
        assertThat(result.cancellationCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("판매와 취소 내역으로부터 totals 집계 + 수수료율 적용한 PENDING 정산을 산출한다")
    void resolve_calculates_PENDING_from_sales_and_cancellation_aggregates() {
        // Given
        CreatorId creatorId = CreatorId.of(81L);
        CourseId courseId = CourseId.of(810L);
        YearMonth yearMonth = YearMonth.of(2026, 4);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의 A"));

        SalesRecord sale1 = SalesRecord.of(courseId, StudentId.of(10L), Money.of(new BigDecimal("30000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 5, 10, 0)));
        SalesRecord sale2 = SalesRecord.of(courseId, StudentId.of(11L), Money.of(new BigDecimal("20000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 15, 10, 0)));
        salesRepository.saveSalesRecord(sale1);
        salesRepository.saveSalesRecord(sale2);

        CancellationRecord cancellation = CancellationRecord.of(SalesRecordId.of(1L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 20, 10, 0)));
        salesRepository.saveCancellationRecord(cancellation);

        // When
        Settlement result = resolver.resolve(creatorId, yearMonth);

        // Then
        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.totalSales().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
        assertThat(result.totalRefund().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("10000"));
        assertThat(result.salesCount()).isEqualTo(2L);
        assertThat(result.cancellationCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("yearMonth의 1일에 유효한 수수료율을 조회해 정산에 반영한다")
    void resolve_applies_effective_fee_rate_from_yearMonth_first_day() {
        // Given
        CreatorId creatorId = CreatorId.of(82L);
        feePolicyRepository.save(FeePolicy.of(FeeRate.of(new BigDecimal("0.3")), LocalDate.of(2026, 3, 1)));
        YearMonth yearMonth = YearMonth.of(2026, 4);

        // When
        Settlement result = resolver.resolve(creatorId, yearMonth);

        // Then
        assertThat(result.feeRate().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("0.3"));
    }
}
