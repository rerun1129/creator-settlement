package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.application.fee.FeePolicyServiceImpl;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.creator.Creator;
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
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryCreatorRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySettlementRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettlementService 단위 테스트")
class SettlementServiceTest {

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
        feePolicyService = new FeePolicyServiceImpl(feePolicyRepository);
        feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
        service = new SettlementServiceImpl(
                settlementRepository,
                salesRepository,
                creatorRepository,
                new MonthlySettlementCalculator(),
                new SettlementAmountCalculator(),
                feePolicyService
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
    @DisplayName("정산 대상 월의 effective 정책 rate가 platformFee 산출에 적용된다")
    void applies_effective_policy_rate_when_policy_registered_for_target_month() {
        // Given
        feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 8, 1)));

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

    // --- confirm 시나리오 ---

    @Test
    @DisplayName("PENDING으로 저장된 Settlement에 confirm 호출 시 status가 CONFIRMED로 전이되고 confirmedAt이 보존된다")
    void confirm_changes_status_to_CONFIRMED_when_stored_PENDING() {
        // Given
        CreatorId creatorId = CreatorId.of(50L);
        YearMonth yearMonth = YearMonth.of(2026, 7);
        settlementRepository.save(pendingFixture(creatorId, yearMonth));
        LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 7);
        CourseId courseId = CourseId.of(510L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의E"));

        SalesRecord sale1 = SalesRecord.of(courseId, StudentId.of(10L), Money.of(new BigDecimal("30000")), OccurredAt.of(LocalDateTime.of(2026, 7, 5, 10, 0)));
        SalesRecord sale2 = SalesRecord.of(courseId, StudentId.of(11L), Money.of(new BigDecimal("20000")), OccurredAt.of(LocalDateTime.of(2026, 7, 15, 10, 0)));
        salesRepository.saveSalesRecord(sale1);
        salesRepository.saveSalesRecord(sale2);

        SalesRecordId salesRecordId1 = SalesRecordId.of(1L);
        CancellationRecord cancellation = CancellationRecord.of(salesRecordId1, Money.of(new BigDecimal("10000")), OccurredAt.of(LocalDateTime.of(2026, 7, 20, 10, 0)));
        salesRepository.saveCancellationRecord(cancellation);

        LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 8, 1, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 7);
        settlementRepository.save(confirmedFixture(creatorId, yearMonth));
        LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 8, 2, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 7);
        settlementRepository.save(paidFixture(creatorId, yearMonth));
        LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 8, 3, 9, 0);

        // When & Then
        assertThatThrownBy(() -> service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 지급된 정산입니다");
    }

    @Test
    @DisplayName("confirm 처리 후 Settlement는 repository에 저장되어 후속 조회에서 CONFIRMED로 확인된다")
    void confirm_persists_via_repository_save() {
        // Given
        CreatorId creatorId = CreatorId.of(54L);
        YearMonth yearMonth = YearMonth.of(2026, 7);
        settlementRepository.save(pendingFixture(creatorId, yearMonth));
        LocalDateTime confirmedAtLocalDateTime = LocalDateTime.of(2026, 8, 4, 9, 0);

        // When
        service.confirm(new ConfirmSettlementCommand(creatorId.value(), yearMonth, confirmedAtLocalDateTime));

        // Then
        Settlement persisted = settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth).orElseThrow();
        assertThat(persisted.status()).isEqualTo(SettlementStatus.CONFIRMED);
    }

    // --- pay 시나리오 ---

    @Test
    @DisplayName("CONFIRMED로 저장된 Settlement에 pay 호출 시 status가 PAID로 전이되고 paidAt이 보존된다")
    void pay_changes_status_to_PAID_when_stored_CONFIRMED() {
        // Given
        CreatorId creatorId = CreatorId.of(60L);
        YearMonth yearMonth = YearMonth.of(2026, 8);
        settlementRepository.save(confirmedFixture(creatorId, yearMonth));
        LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 9, 1, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 8);
        LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 9, 1, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 8);
        settlementRepository.save(pendingFixture(creatorId, yearMonth));
        LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 9, 2, 9, 0);

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
        YearMonth yearMonth = YearMonth.of(2026, 8);
        settlementRepository.save(paidFixture(creatorId, yearMonth));
        LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 9, 3, 9, 0);

        // When & Then
        assertThatThrownBy(() -> service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 지급된 정산입니다");
    }

    @Test
    @DisplayName("pay 처리 후 Settlement는 repository에 저장되어 후속 조회에서 PAID로 확인된다")
    void pay_persists_via_repository_save() {
        // Given
        CreatorId creatorId = CreatorId.of(64L);
        YearMonth yearMonth = YearMonth.of(2026, 8);
        settlementRepository.save(confirmedFixture(creatorId, yearMonth));
        LocalDateTime paidAtLocalDateTime = LocalDateTime.of(2026, 9, 4, 9, 0);

        // When
        service.pay(new PaySettlementCommand(creatorId.value(), yearMonth, paidAtLocalDateTime));

        // Then
        Settlement persisted = settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth).orElseThrow();
        assertThat(persisted.status()).isEqualTo(SettlementStatus.PAID);
    }

    // --- getSettlementsInRange 시나리오 ---

    @Test
    @DisplayName("활동이 전혀 없으면 시스템 전체 Creator가 expectedSettlementAmount=0으로 응답되고 totalAmount=0이다")
    void getSettlementsInRange_returns_zero_expected_settlement_amount_for_all_creators_when_no_activity() {
        // given
        creatorRepository.saveCreator(Creator.of(CreatorId.of(1L), "크리에이터1"));
        creatorRepository.saveCreator(Creator.of(CreatorId.of(2L), "크리에이터2"));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        assertThat(response.responses())
                .extracting(CreatorPayableView::creatorId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.responses())
                .allSatisfy(creatorView ->
                        assertThat(creatorView.expectedSettlementAmount())
                                .usingComparator(BigDecimal::compareTo)
                                .isEqualTo(BigDecimal.ZERO)
                );
        assertThat(response.totalAmount()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("활동 없는 Creator도 시스템 전체 목록에 expectedSettlementAmount=0으로 포함된다 (활동 있는 Creator만 양수 금액)")
    void getSettlementsInRange_includes_all_system_creators_with_zero_for_inactive() {
        // given
        CreatorId activeCreatorId = CreatorId.of(10L);
        CreatorId inactiveCreatorId = CreatorId.of(20L);
        creatorRepository.saveCreator(Creator.of(activeCreatorId, "활동크리에이터"));
        creatorRepository.saveCreator(Creator.of(inactiveCreatorId, "휴면크리에이터"));

        CourseId courseId = CourseId.of(100L);
        courseRepository.saveCourse(Course.of(courseId, activeCreatorId, "강의A"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(1L),
                Money.of(new BigDecimal("30000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 10, 10, 0))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        assertThat(response.responses())
                .extracting(CreatorPayableView::creatorId)
                .containsExactlyInAnyOrder(10L, 20L);
        CreatorPayableView activeView = response.responses().stream()
                .filter(view -> view.creatorId().equals(10L))
                .findFirst().orElseThrow();
        CreatorPayableView inactiveView = response.responses().stream()
                .filter(view -> view.creatorId().equals(20L))
                .findFirst().orElseThrow();

        assertThat(activeView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("24000"));
        assertThat(inactiveView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("기간 경계는 KST 일자 기준 [from 00:00, to+1일 00:00)으로 적용된다")
    void getSettlementsInRange_applies_kst_date_boundary() {
        // given
        CreatorId creatorId = CreatorId.of(60L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터60"));

        CourseId courseId = CourseId.of(600L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의D"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(5L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 31, 23, 59))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(6L),
                Money.of(new BigDecimal("80000")),
                OccurredAt.of(LocalDateTime.of(2026, 6, 1, 0, 1))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        CreatorPayableView creatorView = response.responses().stream()
                .filter(view -> view.creatorId().equals(60L))
                .findFirst().orElseThrow();
        assertThat(creatorView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("40000"));
    }

    @Test
    @DisplayName("totalAmount는 모든 Creator의 expectedSettlementAmount 합이다")
    void getSettlementsInRange_sums_total_amount_across_all_creators() {
        // given
        CreatorId creatorA = CreatorId.of(70L);
        CreatorId creatorB = CreatorId.of(71L);
        creatorRepository.saveCreator(Creator.of(creatorA, "크리에이터70"));
        creatorRepository.saveCreator(Creator.of(creatorB, "크리에이터71"));

        CourseId courseA = CourseId.of(700L);
        CourseId courseB = CourseId.of(710L);
        courseRepository.saveCourse(Course.of(courseA, creatorA, "강의E"));
        courseRepository.saveCourse(Course.of(courseB, creatorB, "강의F"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseA, StudentId.of(7L),
                Money.of(new BigDecimal("40000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 10, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseB, StudentId.of(8L),
                Money.of(new BigDecimal("60000")),
                OccurredAt.of(LocalDateTime.of(2026, 6, 12, 10, 0))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        BigDecimal expectedTotal = response.responses().stream()
                .map(CreatorPayableView::expectedSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(response.totalAmount()).usingComparator(BigDecimal::compareTo).isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("기간 밖의 SalesRecord/CancellationRecord는 산정에서 제외된다 (예: from=2026-05-31, to=2026-05-31일 때 5월 1~30일 데이터 미포함)")
    void getSettlementsInRange_excludes_records_outside_date_range() {
        // given
        CreatorId creatorId = CreatorId.of(90L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터90"));

        CourseId courseId = CourseId.of(900L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의G"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(9L),
                Money.of(new BigDecimal("70000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 15, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(10L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2026, 5, 31, 12, 0))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        CreatorPayableView creatorView = response.responses().stream()
                .filter(view -> view.creatorId().equals(90L))
                .findFirst().orElseThrow();
        assertThat(creatorView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("40000"));
    }

    @Test
    @DisplayName("다중 월 range에서 각 월의 effective 정책 rate가 적용된다")
    void getSettlementsInRange_appliesPerMonthPolicyRate_whenMultiMonthRange() {
        // given
        feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 6, 1)));

        CreatorId creatorId = CreatorId.of(99L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터99"));

        CourseId courseId = CourseId.of(990L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의X"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(101L),
                Money.of(new BigDecimal("100000")),
                OccurredAt.of(LocalDateTime.of(2026, 1, 15, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(102L),
                Money.of(new BigDecimal("100000")),
                OccurredAt.of(LocalDateTime.of(2026, 6, 15, 10, 0))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        CreatorPayableView creatorView = response.responses().stream()
                .filter(view -> view.creatorId().equals(99L))
                .findFirst().orElseThrow();
        assertThat(creatorView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("162000"));
        assertThat(response.totalAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("162000"));
    }

    @Test
    @DisplayName("월별 net<0이면 그 달 fee=0으로 독립 산출하여 합산한다")
    void getSettlementsInRange_handlesNegativeMonthIndependently_whenRefundExceedsSales() {
        // given
        CreatorId creatorId = CreatorId.of(99L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터99"));

        CourseId courseId = CourseId.of(990L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의X"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(201L),
                Money.of(new BigDecimal("100000")),
                OccurredAt.of(LocalDateTime.of(2026, 1, 15, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(202L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2026, 6, 15, 10, 0))
        ));

        salesRepository.saveCancellationRecord(CancellationRecord.of(
                SalesRecordId.of(2L),
                Money.of(new BigDecimal("100000")),
                OccurredAt.of(LocalDateTime.of(2026, 6, 20, 10, 0))
        ));

        SettlementRangeQuery query = new SettlementRangeQuery(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        // when
        SettlementRangeView response = service.getSettlementsInRange(query);

        // then
        CreatorPayableView creatorView = response.responses().stream()
                .filter(view -> view.creatorId().equals(99L))
                .findFirst().orElseThrow();
        assertThat(creatorView.expectedSettlementAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("30000"));
        assertThat(response.totalAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("30000"));
    }

    // --- 픽스처 헬퍼 ---

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
                OccurredAt.of(LocalDateTime.of(2026, 8, 1, 9, 0))
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
                OccurredAt.of(LocalDateTime.of(2026, 8, 1, 9, 0)),
                OccurredAt.of(LocalDateTime.of(2026, 9, 1, 9, 0))
        );
    }
}
