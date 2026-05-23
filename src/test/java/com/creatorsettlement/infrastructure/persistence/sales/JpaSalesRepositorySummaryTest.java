package com.creatorsettlement.infrastructure.persistence.sales;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.infrastructure.config.JpaAuditingConfig;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import com.creatorsettlement.infrastructure.persistence.creator.CreatorJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("jpa-test")
@Import({JpaSalesRepository.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSalesRepositorySummaryTest {

    @Autowired
    private SalesRepository sut;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("특정 creator의 해당 월 sales의 SUM과 COUNT를 반환한다")
    void findSalesSummary_returns_sum_and_count_for_creator_month() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(dbCreatorId, "강의 A"));
        em.persistAndFlush(salesEntity(course, new BigDecimal("10000"), LocalDateTime.of(2026, 5, 1, 0, 0)));
        em.persistAndFlush(salesEntity(course, new BigDecimal("20000"), LocalDateTime.of(2026, 5, 15, 0, 0)));
        em.persistAndFlush(salesEntity(course, new BigDecimal("30000"), LocalDateTime.of(2026, 5, 31, 23, 59)));
        em.clear();

        // when
        SalesSummary result = sut.findSalesSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalAmount().value()).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(result.count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("해당 월에 sales가 없으면 0금액·0건을 반환한다")
    void findSalesSummary_returns_zero_when_no_data() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 B")).getId();
        em.clear();

        // when
        SalesSummary result = sut.findSalesSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalAmount().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("다른 creator의 sales는 합산에 포함되지 않는다")
    void findSalesSummary_excludes_other_creators() {
        // given
        Long creatorIdA = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        Long creatorIdB = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 B")).getId();
        CourseJpaEntity courseA = em.persistAndFlush(CourseJpaEntity.of(creatorIdA, "강의 A"));
        CourseJpaEntity courseB = em.persistAndFlush(CourseJpaEntity.of(creatorIdB, "강의 B"));
        em.persistAndFlush(salesEntity(courseA, new BigDecimal("10000"), LocalDateTime.of(2026, 5, 10, 0, 0)));
        em.persistAndFlush(salesEntity(courseB, new BigDecimal("99000"), LocalDateTime.of(2026, 5, 11, 0, 0)));
        em.clear();

        // when
        SalesSummary result = sut.findSalesSummaryByCreatorAndMonth(CreatorId.of(creatorIdA), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalAmount().value()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(result.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 yearMonth의 sales는 합산에 포함되지 않는다")
    void findSalesSummary_excludes_other_months() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(dbCreatorId, "강의 A"));
        em.persistAndFlush(salesEntity(course, new BigDecimal("10000"), LocalDateTime.of(2026, 4, 30, 23, 59)));
        em.persistAndFlush(salesEntity(course, new BigDecimal("20000"), LocalDateTime.of(2026, 5, 1, 0, 0)));
        em.persistAndFlush(salesEntity(course, new BigDecimal("30000"), LocalDateTime.of(2026, 6, 1, 0, 0)));
        em.clear();

        // when
        SalesSummary result = sut.findSalesSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalAmount().value()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(result.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("특정 creator의 해당 월 cancellation의 SUM과 COUNT를 반환한다")
    void findCancellationSummary_returns_sum_and_count_for_creator_month() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(dbCreatorId, "강의 A"));
        SalesRecordJpaEntity sale1 = em.persistAndFlush(salesEntity(course, new BigDecimal("10000"), LocalDateTime.of(2026, 4, 10, 0, 0)));
        SalesRecordJpaEntity sale2 = em.persistAndFlush(salesEntity(course, new BigDecimal("20000"), LocalDateTime.of(2026, 4, 15, 0, 0)));
        em.persistAndFlush(cancellationEntity(sale1.getId(), new BigDecimal("5000"), LocalDateTime.of(2026, 5, 3, 0, 0)));
        em.persistAndFlush(cancellationEntity(sale2.getId(), new BigDecimal("8000"), LocalDateTime.of(2026, 5, 10, 0, 0)));
        em.clear();

        // when
        CancellationSummary result = sut.findCancellationSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalRefund().value()).isEqualByComparingTo(new BigDecimal("13000"));
        assertThat(result.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("환불은 결제월이 아닌 취소월 기준으로 합산된다")
    void findCancellationSummary_refund_attributed_to_cancellation_month_not_payment_month() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(dbCreatorId, "강의 A"));
        SalesRecordJpaEntity sale = em.persistAndFlush(salesEntity(course, new BigDecimal("10000"), LocalDateTime.of(2026, 4, 10, 0, 0)));
        em.persistAndFlush(cancellationEntity(sale.getId(), new BigDecimal("10000"), LocalDateTime.of(2026, 5, 5, 0, 0)));
        em.clear();

        // when
        CancellationSummary aprilResult = sut.findCancellationSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 4));
        CancellationSummary mayResult = sut.findCancellationSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(aprilResult.totalRefund().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(aprilResult.count()).isEqualTo(0L);
        assertThat(mayResult.totalRefund().value()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(mayResult.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("해당 월에 cancellation이 없으면 0금액·0건을 반환한다")
    void findCancellationSummary_returns_zero_when_no_data() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        em.clear();

        // when
        CancellationSummary result = sut.findCancellationSummaryByCreatorAndMonth(CreatorId.of(dbCreatorId), YearMonth.of(2026, 5));

        // then
        assertThat(result.totalRefund().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.count()).isEqualTo(0L);
    }

    private SalesRecordJpaEntity salesEntity(CourseJpaEntity course, BigDecimal amount, LocalDateTime paidAt) {
        return SalesRecordJpaEntity.of(course, 1L, amount, paidAt);
    }

    private CancellationRecordJpaEntity cancellationEntity(Long salesRecordId, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        return CancellationRecordJpaEntity.of(salesRecordId, refundAmount, cancelledAt);
    }
}
