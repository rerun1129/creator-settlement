package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.application.fee.FeePolicyServiceImpl;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
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
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementService 범위 집계 단위 테스트")
class SettlementServiceRangeTest {

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
        service = new SettlementServiceImpl(
                settlementRepository,
                salesRepository,
                creatorRepository,
                new MonthlySettlementCalculator(),
                new SettlementAmountCalculator(),
                feePolicyService,
                settlementExcelWriter,
                monthClosurePolicy
        );
    }

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
        feePolicyService.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 5, 1)));

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
}
