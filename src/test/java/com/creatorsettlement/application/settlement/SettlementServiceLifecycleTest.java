package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.application.fee.FeePolicyServiceImpl;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.course.Course;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettlementService 확정·지급 단위 테스트")
class SettlementServiceLifecycleTest {

    @Nested
    @DisplayName("확정")
    class Confirm {

        private InMemorySettlementRepository settlementRepository;
        private InMemorySalesRepository salesRepository;
        private InMemoryCourseRepository courseRepository;
        private InMemoryCreatorRepository creatorRepository;
        private InMemoryFeePolicyRepository feePolicyRepository;
        private FeePolicyService feePolicyService;
        private SettlementService service;

        @BeforeEach
        void setUp() {
            settlementRepository = new InMemorySettlementRepository();
            courseRepository = new InMemoryCourseRepository();
            creatorRepository = new InMemoryCreatorRepository();
            salesRepository = new InMemorySalesRepository(courseRepository);
            feePolicyRepository = new InMemoryFeePolicyRepository();
            feePolicyService = new FeePolicyServiceImpl(feePolicyRepository, new FeePolicyDomainService(feePolicyRepository));
            feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
            SettlementExcelWriter settlementExcelWriter = new SettlementExcelWriter();
            SettlementMonthClosurePolicy monthClosurePolicy = new SettlementMonthClosurePolicy();
            PendingSettlementResolver pendingSettlementResolver = new PendingSettlementResolver(
                    salesRepository, feePolicyService, new MonthlySettlementCalculator());
            RequiredSettlementResolver requiredSettlementResolver = new RequiredSettlementResolver(settlementRepository);
            SettlementRangePayoutAssembler settlementRangePayoutAssembler = new SettlementRangePayoutAssembler(
                    salesRepository, creatorRepository,
                    new CreatorRangePayoutCalculator(new SettlementAmountCalculator(), feePolicyService));
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
        @DisplayName("PENDING으로 저장된 Settlement에 confirm 호출 시 status가 CONFIRMED로 전이되고 confirmedAt이 보존된다")
        void confirm_changes_status_to_CONFIRMED_when_stored_PENDING() {
            // Given
            CreatorId creatorId = CreatorId.of(50L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(pendingFixture(creatorId, yearMonth));
            LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 5, 1, 9, 0);

            // When
            service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime));

            // Then
            Settlement result = settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth).orElseThrow();
            assertThat(result.status()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(result.confirmedAt().value()).isEqualTo(confirmedAtLocalDateTime);
        }

        @Test
        @DisplayName("저장된 Settlement가 없을 때 confirm 호출 시 sales 집계로 산출 + CONFIRMED 상태로 신규 저장된다")
        void confirm_inserts_CONFIRMED_when_not_stored() {
            // Given
            CreatorId creatorId = CreatorId.of(51L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            CourseId courseId = CourseId.of(510L);
            courseRepository.saveCourse(Course.of(courseId, creatorId, "강의E"));

            SalesRecord sale1 = SalesRecord.of(courseId, StudentId.of(10L), Money.of(new BigDecimal("30000")), OccurredAt.of(LocalDateTime.of(2026, 4, 5, 10, 0)));
            SalesRecord sale2 = SalesRecord.of(courseId, StudentId.of(11L), Money.of(new BigDecimal("20000")), OccurredAt.of(LocalDateTime.of(2026, 4, 15, 10, 0)));
            salesRepository.saveSalesRecord(sale1);
            salesRepository.saveSalesRecord(sale2);

            SalesRecordId salesRecordId1 = SalesRecordId.of(1L);
            CancellationRecord cancellation = CancellationRecord.of(salesRecordId1, Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 4, 20, 10, 0)));
            salesRepository.saveCancellationRecord(cancellation);

            LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 5, 1, 9, 0);

            // When
            service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime));

            // Then
            Settlement persisted = settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth).orElseThrow();
            assertThat(persisted.status()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(persisted.confirmedAt().value()).isEqualTo(confirmedAtLocalDateTime);
            assertThat(persisted.totalSales().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50000"));
            assertThat(persisted.totalRefund().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("10000"));
            assertThat(persisted.salesCount()).isEqualTo(2L);
            assertThat(persisted.cancellationCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("CONFIRMED 상태 Settlement에 confirm 재호출 시 도메인의 이미 확정된 정산 예외가 전파된다")
        void confirm_propagates_ALREADY_CONFIRMED_from_domain() {
            // Given
            CreatorId creatorId = CreatorId.of(52L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(confirmedFixture(creatorId, yearMonth));
            LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 5, 2, 9, 0);

            // When & Then
            assertThatThrownBy(() -> service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 확정된 정산입니다");
        }

        @Test
        @DisplayName("PAID 상태 Settlement에 confirm 호출 시 도메인의 이미 지급된 정산 예외가 전파된다")
        void confirm_propagates_ALREADY_PAID_from_domain() {
            // Given
            CreatorId creatorId = CreatorId.of(53L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(paidFixture(creatorId, yearMonth));
            LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 5, 3, 9, 0);

            // When & Then
            assertThatThrownBy(() -> service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지급된 정산입니다");
        }

        @Test
        @DisplayName("현재 KST 월을 confirm 호출 시 SETTLEMENT_MONTH_IN_PROGRESS 예외가 발생한다")
        void confirm_throws_MONTH_IN_PROGRESS_when_target_is_current_kst_month() {
            // Given
            CreatorId creatorId = CreatorId.of(70L);
            YearMonth current = YearMonth.now(ZoneId.of("Asia/Seoul"));
            LocalDateTime confirmedAt = LocalDateTime.of(2026, 1, 1, 10, 0);

            // When & Then
            assertThatThrownBy(() -> service.confirm(new ConfirmSettlementCommand(creatorId.value(), current, confirmedAt)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(DomainErrorMessage.SETTLEMENT_MONTH_IN_PROGRESS.message());
        }

        @Test
        @DisplayName("미래 월을 confirm 호출 시 SETTLEMENT_MONTH_IN_PROGRESS 예외가 발생한다")
        void confirm_throws_MONTH_IN_PROGRESS_when_target_is_future() {
            // Given
            CreatorId creatorId = CreatorId.of(71L);
            YearMonth future = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
            LocalDateTime confirmedAt = LocalDateTime.of(2026, 1, 1, 10, 0);

            // When & Then
            assertThatThrownBy(() -> service.confirm(new ConfirmSettlementCommand(creatorId.value(), future, confirmedAt)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(DomainErrorMessage.SETTLEMENT_MONTH_IN_PROGRESS.message());
        }

        private Settlement pendingFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.pendingSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L
            );
        }

        private Settlement confirmedFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.confirmedSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L,
                    OccurredAt.of(LocalDateTime.of(2026, 5, 1, 9, 0))
            );
        }

        private Settlement paidFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.paidSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L,
                    OccurredAt.of(LocalDateTime.of(2026, 5, 1, 9, 0)),
                    OccurredAt.of(LocalDateTime.of(2026, 5, 2, 9, 0))
            );
        }
    }

    @Nested
    @DisplayName("지급")
    class Pay {

        private InMemorySettlementRepository settlementRepository;
        private InMemorySalesRepository salesRepository;
        private InMemoryCourseRepository courseRepository;
        private InMemoryCreatorRepository creatorRepository;
        private InMemoryFeePolicyRepository feePolicyRepository;
        private FeePolicyService feePolicyService;
        private SettlementService service;

        @BeforeEach
        void setUp() {
            settlementRepository = new InMemorySettlementRepository();
            courseRepository = new InMemoryCourseRepository();
            creatorRepository = new InMemoryCreatorRepository();
            salesRepository = new InMemorySalesRepository(courseRepository);
            feePolicyRepository = new InMemoryFeePolicyRepository();
            feePolicyService = new FeePolicyServiceImpl(feePolicyRepository, new FeePolicyDomainService(feePolicyRepository));
            feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
            SettlementExcelWriter settlementExcelWriter = new SettlementExcelWriter();
            SettlementMonthClosurePolicy monthClosurePolicy = new SettlementMonthClosurePolicy();
            PendingSettlementResolver pendingSettlementResolver = new PendingSettlementResolver(
                    salesRepository, feePolicyService, new MonthlySettlementCalculator());
            RequiredSettlementResolver requiredSettlementResolver = new RequiredSettlementResolver(settlementRepository);
            SettlementRangePayoutAssembler settlementRangePayoutAssembler = new SettlementRangePayoutAssembler(
                    salesRepository, creatorRepository,
                    new CreatorRangePayoutCalculator(new SettlementAmountCalculator(), feePolicyService));
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
        @DisplayName("CONFIRMED로 저장된 Settlement에 pay 호출 시 status가 PAID로 전이되고 paidAt이 보존된다")
        void pay_changes_status_to_PAID_when_stored_CONFIRMED() {
            // Given
            CreatorId creatorId = CreatorId.of(60L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(confirmedFixture(creatorId, yearMonth));
            LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 5, 1, 9, 0);

            // When
            service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime));

            // Then
            Settlement result = settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth).orElseThrow();
            assertThat(result.status()).isEqualTo(SettlementStatus.PAID);
            assertThat(result.paidAt().value()).isEqualTo(paidAtLocalDateTime);
        }

        @Test
        @DisplayName("저장된 Settlement 없을 때 pay 호출 시 정산 내역을 찾을 수 없다는 예외가 발생한다")
        void pay_throws_SETTLEMENT_NOT_FOUND_when_not_stored() {
            // Given
            CreatorId creatorId = CreatorId.of(61L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 5, 1, 9, 0);

            // When & Then
            assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("정산 내역을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("PENDING 상태 Settlement에 pay 호출 시 도메인의 확정되지 않은 정산 예외가 전파된다")
        void pay_propagates_NOT_CONFIRMED_FOR_PAYMENT_from_domain_when_PENDING() {
            // Given
            CreatorId creatorId = CreatorId.of(62L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(pendingFixture(creatorId, yearMonth));
            LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 5, 2, 9, 0);

            // When & Then
            assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("확정되지 않은 정산은 지급할 수 없습니다");
        }

        @Test
        @DisplayName("PAID 상태 Settlement에 pay 재호출 시 도메인의 이미 지급된 정산 예외가 전파된다")
        void pay_propagates_ALREADY_PAID_from_domain() {
            // Given
            CreatorId creatorId = CreatorId.of(63L);
            YearMonth yearMonth = YearMonth.of(2026, 4);
            settlementRepository.save(paidFixture(creatorId, yearMonth));
            LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 5, 3, 9, 0);

            // When & Then
            assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지급된 정산입니다");
        }

        @Test
        @DisplayName("현재 KST 월을 pay 호출 시 SETTLEMENT_MONTH_IN_PROGRESS 예외가 발생한다")
        void pay_throws_MONTH_IN_PROGRESS_when_target_is_current_kst_month() {
            // Given
            CreatorId creatorId = CreatorId.of(72L);
            YearMonth current = YearMonth.now(ZoneId.of("Asia/Seoul"));
            LocalDateTime paidAt = LocalDateTime.of(2026, 1, 1, 10, 0);

            // When & Then
            assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), current, paidAt)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(DomainErrorMessage.SETTLEMENT_MONTH_IN_PROGRESS.message());
        }

        @Test
        @DisplayName("미래 월을 pay 호출 시 SETTLEMENT_MONTH_IN_PROGRESS 예외가 발생한다")
        void pay_throws_MONTH_IN_PROGRESS_when_target_is_future() {
            // Given
            CreatorId creatorId = CreatorId.of(73L);
            YearMonth future = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
            LocalDateTime paidAt = LocalDateTime.of(2026, 1, 1, 10, 0);

            // When & Then
            assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), future, paidAt)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(DomainErrorMessage.SETTLEMENT_MONTH_IN_PROGRESS.message());
        }

        private Settlement pendingFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.pendingSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L
            );
        }

        private Settlement confirmedFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.confirmedSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L,
                    OccurredAt.of(LocalDateTime.of(2026, 5, 1, 9, 0))
            );
        }

        private Settlement paidFixture(CreatorId creatorId, YearMonth yearMonth) {
            return Settlement.paidSnapshot(
                    creatorId, yearMonth,
                    Money.of(new BigDecimal("100000")),
                    Money.of(new BigDecimal("10000")),
                    SettlementAmount.of(new BigDecimal("90000")),
                    FeeRate.defaultRate(),
                    Money.of(new BigDecimal("18000")),
                    SettlementAmount.of(new BigDecimal("72000")),
                    5L, 1L,
                    OccurredAt.of(LocalDateTime.of(2026, 5, 1, 9, 0)),
                    OccurredAt.of(LocalDateTime.of(2026, 5, 2, 9, 0))
            );
        }
    }
}
