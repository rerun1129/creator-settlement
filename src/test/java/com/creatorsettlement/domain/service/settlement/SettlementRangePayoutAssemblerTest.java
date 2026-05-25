package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.application.fee.FeePolicyServiceImpl;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.service.fee.FeePolicyDomainService;
import com.creatorsettlement.domain.service.settlement.dto.CreatorRangePayout;
import com.creatorsettlement.domain.service.settlement.dto.SettlementRangePayoutResult;
import com.creatorsettlement.infrastructure.persistence.InMemoryCourseRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryCreatorRepository;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import com.creatorsettlement.infrastructure.persistence.InMemorySalesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementRangePayoutAssembler 단위 테스트")
class SettlementRangePayoutAssemblerTest {

    private InMemoryCourseRepository courseRepository;
    private InMemoryCreatorRepository creatorRepository;
    private InMemorySalesRepository salesRepository;
    private InMemoryFeePolicyRepository feePolicyRepository;
    private SettlementRangePayoutAssembler assembler;

    @BeforeEach
    void setUp() {
        courseRepository = new InMemoryCourseRepository();
        creatorRepository = new InMemoryCreatorRepository();
        salesRepository = new InMemorySalesRepository(courseRepository);
        feePolicyRepository = new InMemoryFeePolicyRepository();
        FeePolicyDomainService feePolicyDomainService = new FeePolicyDomainService(feePolicyRepository);
        FeePolicyServiceImpl feePolicyService = new FeePolicyServiceImpl(feePolicyRepository, feePolicyDomainService);
        feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
        CreatorRangePayoutCalculator creatorRangePayoutCalculator = new CreatorRangePayoutCalculator(
                new SettlementAmountCalculator());
        assembler = new SettlementRangePayoutAssembler(salesRepository, creatorRepository, creatorRangePayoutCalculator, feePolicyDomainService);
    }

    @Test
    @DisplayName("등록된 크리에이터가 없으면 빈 payouts와 totalAmount 0 반환")
    void assemble_returns_empty_payouts_and_zero_total_when_no_creators() {
        // Given
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        // When
        SettlementRangePayoutResult result = assembler.assemble(from, to);

        // Then
        assertThat(result.payouts()).isEmpty();
        assertThat(result.totalAmount()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("크리에이터는 등록되어 있지만 해당 기간 sales가 없으면 0원 payout 반환")
    void assemble_returns_zero_payout_when_creator_has_no_sales_in_range() {
        // Given
        CreatorId creatorId = CreatorId.of(1L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터1"));

        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        // When
        SettlementRangePayoutResult result = assembler.assemble(from, to);

        // Then
        assertThat(result.payouts()).hasSize(1);
        CreatorRangePayout payout = result.payouts().get(0);
        assertThat(payout.creatorId()).isEqualTo(creatorId);
        assertThat(payout.expectedPayout()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalAmount()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("단일 크리에이터 + 단일 월 sales/cancellation → 정확한 expected payout과 totalAmount 반환")
    void assemble_calculates_single_creator_single_month_payout() {
        // Given
        CreatorId creatorId = CreatorId.of(1L);
        creatorRepository.saveCreator(Creator.of(creatorId, "크리에이터1"));

        CourseId courseId = CourseId.of(100L);
        courseRepository.saveCourse(Course.of(courseId, creatorId, "강의A"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseId, StudentId.of(1L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 10, 10, 0))
        ));
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                SalesRecordId.of(1L),
                Money.of(new BigDecimal("10000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 15, 12, 0))
        ));

        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        // When
        SettlementRangePayoutResult result = assembler.assemble(from, to);

        // Then
        assertThat(result.payouts()).hasSize(1);
        CreatorRangePayout payout = result.payouts().get(0);
        assertThat(payout.creatorId()).isEqualTo(creatorId);
        assertThat(payout.expectedPayout())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("32000"));
        assertThat(result.totalAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("32000"));
    }

    @Test
    @DisplayName("다중 크리에이터 + 다중 월에 걸친 sales/cancellation을 creator별·월별로 합산해 totalAmount 반환")
    void assemble_sums_payouts_across_multiple_creators_and_months() {
        // Given
        CreatorId creatorA = CreatorId.of(10L);
        CreatorId creatorB = CreatorId.of(20L);
        creatorRepository.saveCreator(Creator.of(creatorA, "크리에이터A"));
        creatorRepository.saveCreator(Creator.of(creatorB, "크리에이터B"));

        CourseId courseA = CourseId.of(100L);
        CourseId courseB = CourseId.of(200L);
        courseRepository.saveCourse(Course.of(courseA, creatorA, "강의A"));
        courseRepository.saveCourse(Course.of(courseB, creatorB, "강의B"));

        salesRepository.saveSalesRecord(SalesRecord.of(
                courseA, StudentId.of(1L),
                Money.of(new BigDecimal("100000")),
                OccurredAt.of(LocalDateTime.of(2026, 3, 10, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseA, StudentId.of(2L),
                Money.of(new BigDecimal("50000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 10, 10, 0))
        ));
        salesRepository.saveSalesRecord(SalesRecord.of(
                courseB, StudentId.of(3L),
                Money.of(new BigDecimal("80000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 12, 10, 0))
        ));
        salesRepository.saveCancellationRecord(CancellationRecord.of(
                SalesRecordId.of(3L),
                Money.of(new BigDecimal("20000")),
                OccurredAt.of(LocalDateTime.of(2026, 4, 20, 12, 0))
        ));

        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        // When
        SettlementRangePayoutResult result = assembler.assemble(from, to);

        // Then
        assertThat(result.payouts()).hasSize(2);
        CreatorRangePayout payoutA = result.payouts().stream()
                .filter(p -> p.creatorId().equals(creatorA))
                .findFirst().orElseThrow();
        CreatorRangePayout payoutB = result.payouts().stream()
                .filter(p -> p.creatorId().equals(creatorB))
                .findFirst().orElseThrow();
        assertThat(payoutA.expectedPayout())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("120000"));
        assertThat(payoutB.expectedPayout())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("48000"));
        assertThat(result.totalAmount())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("168000"));
    }
}
