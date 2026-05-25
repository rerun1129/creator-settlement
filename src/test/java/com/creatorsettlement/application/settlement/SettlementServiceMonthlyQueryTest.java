package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
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
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.service.fee.FeePolicyDomainService;
import com.creatorsettlement.domain.service.settlement.CreatorRangePayoutCalculator;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.PendingSettlementResolver;
import com.creatorsettlement.domain.service.settlement.RequiredSettlementResolver;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementRangePayoutAssembler;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryCreatorRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySettlementRepository;
import com.creatorsettlement.infrastructure.settlement.excel.SettlementExcelWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementService 월별 조회 단위 테스트")
class SettlementServiceMonthlyQueryTest {

    private InMemorySettlementRepository settlementRepository;
    private InMemorySalesRepository salesRepository;
    private InMemoryCourseRepository courseRepository;
    private InMemoryCreatorRepository creatorRepository;
    private InMemoryFeePolicyRepository feePolicyRepository;
    private SettlementService service;

    @BeforeEach
    void setUp() {
        settlementRepository = new InMemorySettlementRepository();
        courseRepository = new InMemoryCourseRepository();
        creatorRepository = new InMemoryCreatorRepository();
        salesRepository = new InMemorySalesRepository(courseRepository);
        feePolicyRepository = new InMemoryFeePolicyRepository();
        FeePolicyDomainService feePolicyDomainService = new FeePolicyDomainService(feePolicyRepository);
        feePolicyRepository.save(FeePolicy.of(FeeRate.of(new BigDecimal("0.2")), LocalDate.of(2020, 1, 1)));
        SettlementExcelWriter settlementExcelWriter = new SettlementExcelWriter();
        SettlementMonthClosurePolicy monthClosurePolicy = new SettlementMonthClosurePolicy();
        PendingSettlementResolver pendingSettlementResolver = new PendingSettlementResolver(
                salesRepository, feePolicyDomainService, new MonthlySettlementCalculator());
        RequiredSettlementResolver requiredSettlementResolver = new RequiredSettlementResolver(settlementRepository);
        SettlementRangePayoutAssembler settlementRangePayoutAssembler = new SettlementRangePayoutAssembler(
                salesRepository, creatorRepository,
                new CreatorRangePayoutCalculator(new SettlementAmountCalculator()), feePolicyDomainService);
        service = new SettlementServiceImpl(
                settlementRepository,
                settlementExcelWriter,
                monthClosurePolicy,
                pendingSettlementResolver,
                requiredSettlementResolver,
                settlementRangePayoutAssembler
        );
    }

    @Test
    @DisplayName("저장된 Settlement가 있으면 그 스냅샷을 응답으로 반환한다")
    void returns_stored_snapshot_when_settlement_exists() {
        // Given
        CreatorId creatorId = CreatorId.of(1L);
        YearMonth yearMonth = YearMonth.of(2026, 4);
        Settlement stored = Settlement.pendingSnapshot(
                creatorId, yearMonth,
                Money.of(new BigDecimal("50000")),
                Money.of(new BigDecimal("5000")),
                SettlementAmount.of(new BigDecimal("45000")),
                FeeRate.defaultRate(),
                Money.of(new BigDecimal("9000")),
                SettlementAmount.of(new BigDecimal("36000")),
                3L, 1L
        );
        settlementRepository.save(stored);

        MonthlySettlementQuery command = new MonthlySettlementQuery(1L, yearMonth);

        // When
        MonthlySettlementView response = service.getMonthlySettlement(command);

        // Then
        assertThat(response.creatorId()).isEqualTo(1L);
        assertThat(response.yearMonth()).isEqualTo(yearMonth);
        assertThat(response.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(response.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
        assertThat(response.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("5000"));
        assertThat(response.netSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("45000"));
        assertThat(response.platformFee()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("9000"));
        assertThat(response.expectedPayout()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("36000"));
        assertThat(response.salesCount()).isEqualTo(3L);
        assertThat(response.cancellationCount()).isEqualTo(1L);
        assertThat(response.confirmedAt()).isNull();
    }

    @Test
    @DisplayName("저장된 게 없으면 sales 집계로 PENDING 산출한다")
    void computes_pending_when_no_stored_settlement() {
        // Given
        CreatorId creatorId = CreatorId.of(10L);
        YearMonth yearMonth = YearMonth.of(2026, 5);
        CourseId courseId = CourseId.of(100L);

        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의A"));

        SalesRecord sale1 = SalesRecord.of(courseId, StudentId.of(1L), Money.of(new BigDecimal("30000")), OccurredAt.of(LocalDateTime.of(2026, 5, 10, 10, 0)));
        SalesRecord sale2 = SalesRecord.of(courseId, StudentId.of(2L), Money.of(new BigDecimal("20000")), OccurredAt.of(LocalDateTime.of(2026, 5, 20, 10, 0)));
        salesRepository.saveSalesRecord(sale1);
        salesRepository.saveSalesRecord(sale2);

        SalesRecordId salesRecordId1 = SalesRecordId.of(1L);
        CancellationRecord cancellation = CancellationRecord.of(salesRecordId1, Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 5, 15, 10, 0)));
        salesRepository.saveCancellationRecord(cancellation);

        MonthlySettlementQuery command = new MonthlySettlementQuery(10L, yearMonth);

        // When
        MonthlySettlementView response = service.getMonthlySettlement(command);

        // Then
        assertThat(response.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(response.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
        assertThat(response.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("10000"));
        assertThat(response.salesCount()).isEqualTo(2L);
        assertThat(response.cancellationCount()).isEqualTo(1L);
        assertThat(response.feeRate()).usingComparator(BigDecimal::compareTo).isEqualTo(FeeRate.defaultRate().value());
    }

    @Test
    @DisplayName("정산 대상 월에 적용 가능한 정책(전월 이전 등록) rate가 platformFee 산출에 적용된다")
    void applies_effective_policy_rate_when_policy_registered_for_target_month() {
        // Given
        feePolicyRepository.save(FeePolicy.of(FeeRate.of(new BigDecimal("0.18")), LocalDate.of(2026, 7, 1)));

        CreatorId creatorId = CreatorId.of(99L);
        YearMonth yearMonth = YearMonth.of(2026, 8);
        CourseId courseId = CourseId.of(990L);

        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의X"));

        SalesRecord sale = SalesRecord.of(courseId, StudentId.of(99L), Money.of(new BigDecimal("100000")), OccurredAt.of(LocalDateTime.of(2026, 8, 10, 10, 0)));
        salesRepository.saveSalesRecord(sale);

        MonthlySettlementQuery command = new MonthlySettlementQuery(99L, yearMonth);

        // When
        MonthlySettlementView response = service.getMonthlySettlement(command);

        // Then
        assertThat(response.feeRate()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("0.18"));
        assertThat(response.platformFee()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("18000"));
    }

    @Test
    @DisplayName("활동 없는 월은 0금액 PENDING을 반환한다")
    void returns_zero_pending_when_no_activity() {
        // Given
        MonthlySettlementQuery command = new MonthlySettlementQuery(99L, YearMonth.of(2026, 3));

        // When
        MonthlySettlementView response = service.getMonthlySettlement(command);

        // Then
        assertThat(response.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(response.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(response.salesCount()).isEqualTo(0L);
        assertThat(response.cancellationCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("환불은 결제월이 아닌 취소월에 합산된다")
    void refund_attributed_to_cancellation_month_not_payment_month() {
        // Given
        CreatorId creatorId = CreatorId.of(20L);
        CourseId courseId = CourseId.of(200L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의B"));

        SalesRecord sale = SalesRecord.of(courseId, StudentId.of(3L), Money.of(new BigDecimal("40000")), OccurredAt.of(LocalDateTime.of(2026, 4, 15, 10, 0)));
        salesRepository.saveSalesRecord(sale);

        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        CancellationRecord cancellation = CancellationRecord.of(salesRecordId, Money.of(new BigDecimal("40000")), OccurredAt.of(LocalDateTime.of(2026, 5, 3, 10, 0)));
        salesRepository.saveCancellationRecord(cancellation);

        // When
        MonthlySettlementView aprilResponse = service.getMonthlySettlement(new MonthlySettlementQuery(20L, YearMonth.of(2026, 4)));
        MonthlySettlementView mayResponse = service.getMonthlySettlement(new MonthlySettlementQuery(20L, YearMonth.of(2026, 5)));

        // Then
        assertThat(aprilResponse.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("40000"));
        assertThat(aprilResponse.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);

        assertThat(mayResponse.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(mayResponse.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("40000"));
    }

    @Test
    @DisplayName("CONFIRMED 정산 이후 도착한 환불은 본 정산 불변, 다음 달 PENDING에 음수 반영")
    void late_cancellation_after_confirmed_settlement_keeps_snapshot_and_reflects_in_next_month() {
        // Given
        CreatorId creatorId = CreatorId.of(50L);
        CourseId courseId = CourseId.of(500L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의LATE"));

        SalesRecord sale = SalesRecord.of(
                courseId, StudentId.of(50L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2020, 1, 15, 10, 0))
        );
        salesRepository.saveSalesRecord(sale);

        service.confirm(new ConfirmSettlementCommand(
                50L, YearMonth.of(2020, 1),
                LocalDateTime.of(2020, 2, 15, 10, 0)
        ));

        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        CancellationRecord lateCancellation = CancellationRecord.of(
                salesRecordId,
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2020, 2, 20, 10, 0))
        );
        salesRepository.saveCancellationRecord(lateCancellation);

        // When
        MonthlySettlementView janResponse = service.getMonthlySettlement(
                new MonthlySettlementQuery(50L, YearMonth.of(2020, 1)));
        MonthlySettlementView febResponse = service.getMonthlySettlement(
                new MonthlySettlementQuery(50L, YearMonth.of(2020, 2)));

        // Then
        assertThat(janResponse.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(janResponse.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
        assertThat(janResponse.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);

        assertThat(febResponse.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(febResponse.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(febResponse.totalRefund()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
        assertThat(febResponse.netSales()).usingComparator(BigDecimal::compareTo).isNegative();
    }

    @Test
    @DisplayName("다른 크리에이터의 활동은 영향 없다")
    void ignores_other_creators_activity() {
        // Given
        CreatorId creatorA = CreatorId.of(30L);
        CreatorId creatorB = CreatorId.of(40L);
        CourseId courseA = CourseId.of(300L);
        CourseId courseB = CourseId.of(400L);
        YearMonth yearMonth = YearMonth.of(2026, 6);

        courseRepository.saveCourse(Course.of(courseA, creatorA, "강의C"));
        courseRepository.saveCourse(Course.of(courseB, creatorB, "강의D"));

        salesRepository.saveSalesRecord(SalesRecord.of(courseA, StudentId.of(4L), Money.of(new BigDecimal("15000")), OccurredAt.of(LocalDateTime.of(2026, 6, 5, 10, 0))));
        salesRepository.saveSalesRecord(SalesRecord.of(courseB, StudentId.of(5L), Money.of(new BigDecimal("99000")), OccurredAt.of(LocalDateTime.of(2026, 6, 10, 10, 0))));

        // When
        MonthlySettlementView responseA = service.getMonthlySettlement(new MonthlySettlementQuery(30L, yearMonth));

        // Then
        assertThat(responseA.totalSales()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("15000"));
        assertThat(responseA.salesCount()).isEqualTo(1L);
    }
}
