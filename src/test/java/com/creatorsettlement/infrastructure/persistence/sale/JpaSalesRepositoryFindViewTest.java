package com.creatorsettlement.infrastructure.persistence.sale;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRecordView;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("jpa-test")
@Import(JpaSalesRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSalesRepositoryFindViewTest {

    @Autowired
    private SalesRepository sut;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("creator 기간 조회 시 각 sales의 cancellations가 record별로 정확히 분리되어 반환된다")
    void findSalesView_groupsCancellationsByRecord() {
        // given
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(1L, 100L, "강의 A"));
        SalesRecordJpaEntity sale1 = em.persistAndFlush(salesEntity(course, LocalDateTime.of(2026, 4, 10, 10, 0)));
        SalesRecordJpaEntity sale2 = em.persistAndFlush(salesEntity(course, LocalDateTime.of(2026, 4, 15, 10, 0)));

        em.persistAndFlush(cancellationEntity(sale1.getId(), LocalDateTime.of(2026, 4, 11, 10, 0)));
        em.persistAndFlush(cancellationEntity(sale1.getId(), LocalDateTime.of(2026, 4, 12, 10, 0)));
        em.persistAndFlush(cancellationEntity(sale2.getId(), LocalDateTime.of(2026, 4, 16, 10, 0)));
        em.clear();

        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2026, 5, 1, 0, 0);

        // when
        List<SalesRecordView> result = sut.findSalesView(Optional.of(CreatorId.of(100L)), from, toExclusive);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(v -> v.creatorId().equals(CreatorId.of(100L)));

        SalesRecordView viewSale1 = result.stream()
                .filter(v -> v.id().equals(SalesRecordId.of(sale1.getId())))
                .findFirst()
                .orElseThrow();
        SalesRecordView viewSale2 = result.stream()
                .filter(v -> v.id().equals(SalesRecordId.of(sale2.getId())))
                .findFirst()
                .orElseThrow();

        assertThat(viewSale1.cancellations()).hasSize(2);
        assertThat(viewSale2.cancellations()).hasSize(1);
    }

    @Test
    @DisplayName("creatorId 필터링 시 다른 creator의 sales는 반환되지 않는다")
    void findSalesView_returnsOnlyMatchingCreatorSales_whenMultipleCreatorsExist() {
        // given
        CourseJpaEntity courseA = em.persistAndFlush(CourseJpaEntity.of(10L, 100L, "강의 A"));
        CourseJpaEntity courseB = em.persistAndFlush(CourseJpaEntity.of(11L, 200L, "강의 B"));

        em.persistAndFlush(salesEntity(courseA, LocalDateTime.of(2026, 4, 10, 10, 0)));
        em.persistAndFlush(salesEntity(courseA, LocalDateTime.of(2026, 4, 12, 10, 0)));
        em.persistAndFlush(salesEntity(courseB, LocalDateTime.of(2026, 4, 11, 10, 0)));
        em.clear();

        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2026, 5, 1, 0, 0);

        // when
        List<SalesRecordView> result = sut.findSalesView(Optional.of(CreatorId.of(100L)), from, toExclusive);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(v -> v.creatorId().equals(CreatorId.of(100L)));
    }

    @Test
    @DisplayName("paidAt이 from 직전이거나 toExclusive 경계 이후인 sales는 반환되지 않는다")
    void findSalesView_excludesSalesOutsidePeriodBoundaries() {
        // given
        CourseJpaEntity course = em.persistAndFlush(CourseJpaEntity.of(20L, 100L, "강의 A"));

        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2026, 5, 1, 0, 0);

        em.persistAndFlush(salesEntity(course, from.minusSeconds(1)));
        SalesRecordJpaEntity atFrom = em.persistAndFlush(salesEntity(course, from));
        SalesRecordJpaEntity insidePeriod = em.persistAndFlush(salesEntity(course, LocalDateTime.of(2026, 4, 15, 10, 0)));
        em.persistAndFlush(salesEntity(course, toExclusive));
        em.clear();

        // when
        List<SalesRecordView> result = sut.findSalesView(Optional.of(CreatorId.of(100L)), from, toExclusive);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SalesRecordView::id)
                .containsExactlyInAnyOrder(
                        SalesRecordId.of(atFrom.getId()),
                        SalesRecordId.of(insidePeriod.getId())
                );
    }

    private SalesRecordJpaEntity salesEntity(CourseJpaEntity course, LocalDateTime paidAt) {
        return SalesRecordJpaEntity.of(course, 1L, new BigDecimal("10000"), paidAt);
    }

    private CancellationRecordJpaEntity cancellationEntity(Long salesRecordId, LocalDateTime cancelledAt) {
        return CancellationRecordJpaEntity.of(salesRecordId, new BigDecimal("1000"), cancelledAt);
    }
}
