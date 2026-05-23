package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.settlement.SettlementStatus;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.infrastructure.config.JpaAuditingConfig;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("jpa-test")
@Import({JpaSettlementRepository.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSettlementRepositoryTest {

    @Autowired
    private SettlementRepository sut;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("PENDING Settlement save 후 find하면 동일 필드로 복원된다")
    void save_and_find_pending_round_trip() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);
        Settlement pending = pendingSettlement(creatorId, yearMonth);

        // when
        sut.save(pending);
        em.flush();
        em.clear();

        // then
        Optional<Settlement> found = sut.findByCreatorIdAndYearMonth(creatorId, yearMonth);
        assertThat(found).isPresent();
        Settlement restored = found.get();
        assertThat(restored.creatorId()).isEqualTo(creatorId);
        assertThat(restored.yearMonth()).isEqualTo(yearMonth);
        assertThat(restored.status()).isEqualTo(pending.status());
        assertThat(restored.totalSales().value()).isEqualByComparingTo(pending.totalSales().value());
        assertThat(restored.totalRefund().value()).isEqualByComparingTo(pending.totalRefund().value());
        assertThat(restored.netSales().value()).isEqualByComparingTo(pending.netSales().value());
        assertThat(restored.feeRate().value()).isEqualByComparingTo(pending.feeRate().value());
        assertThat(restored.platformFee().value()).isEqualByComparingTo(pending.platformFee().value());
        assertThat(restored.expectedPayout().value()).isEqualByComparingTo(pending.expectedPayout().value());
        assertThat(restored.salesCount()).isEqualTo(pending.salesCount());
        assertThat(restored.cancellationCount()).isEqualTo(pending.cancellationCount());
        assertThat(restored.confirmedAt()).isNull();
    }

    @Test
    @DisplayName("CONFIRMED Settlement save 후 find하면 confirmedAt까지 복원된다")
    void save_and_find_confirmed_round_trip() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 B")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);
        LocalDateTime confirmedTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        Settlement confirmed = confirmedSettlement(creatorId, yearMonth, confirmedTime);

        // when
        sut.save(confirmed);
        em.flush();
        em.clear();

        // then
        Optional<Settlement> found = sut.findByCreatorIdAndYearMonth(creatorId, yearMonth);
        assertThat(found).isPresent();
        Settlement restored = found.get();
        assertThat(restored.status()).isEqualTo(confirmed.status());
        assertThat(restored.confirmedAt()).isNotNull();
        assertThat(restored.confirmedAt().value()).isEqualTo(confirmedTime);
    }

    @Test
    @DisplayName("creator·yearMonth가 일치하지 않으면 Optional.empty를 반환한다")
    void find_returns_empty_when_no_match() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 C")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);

        // when
        Optional<Settlement> found = sut.findByCreatorIdAndYearMonth(creatorId, yearMonth);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("PAID Settlement save 후 find하면 confirmedAt·paidAt까지 동일하게 복원된다")
    void save_and_find_paid_round_trip() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 D")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);
        LocalDateTime confirmedTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime paidTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        Settlement paid = paidSettlement(creatorId, yearMonth, confirmedTime, paidTime);

        // when
        sut.save(paid);
        em.flush();
        em.clear();

        // then
        Optional<Settlement> found = sut.findByCreatorIdAndYearMonth(creatorId, yearMonth);
        assertThat(found).isPresent();
        Settlement restored = found.get();
        assertThat(restored.status()).isEqualTo(SettlementStatus.PAID);
        assertThat(restored.confirmedAt()).isNotNull();
        assertThat(restored.confirmedAt().value()).isEqualTo(confirmedTime);
        assertThat(restored.paidAt()).isNotNull();
        assertThat(restored.paidAt().value()).isEqualTo(paidTime);
    }

    @Test
    @DisplayName("동일 키로 CONFIRMED 저장 후 PAID를 save하면 paidAt이 갱신된다")
    void save_updates_existing_row_when_same_key_confirmed_to_paid() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 F")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);
        LocalDateTime confirmedTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime paidTime = LocalDateTime.of(2026, 6, 10, 12, 0);

        sut.save(confirmedSettlement(creatorId, yearMonth, confirmedTime));
        em.flush();

        // when
        sut.save(paidSettlement(creatorId, yearMonth, confirmedTime, paidTime));
        em.flush();
        em.clear();

        // then
        List<SettlementJpaEntity> rows = em.getEntityManager()
                .createQuery("SELECT s FROM SettlementJpaEntity s WHERE s.creatorId = :cid AND s.yearMonth = :ym", SettlementJpaEntity.class)
                .setParameter("cid", dbCreatorId)
                .setParameter("ym", "202605")
                .getResultList();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo(SettlementStatus.PAID);
        assertThat(rows.get(0).getPaidAt()).isEqualTo(paidTime);
    }

    private Settlement pendingSettlement(CreatorId creatorId, YearMonth yearMonth) {
        return Settlement.pendingSnapshot(
            creatorId, yearMonth,
            Money.of(new BigDecimal("300000")),
            Money.of(new BigDecimal("10000")),
            SettlementAmount.of(new BigDecimal("290000")),
            FeeRate.of(new BigDecimal("0.2000")),
            Money.of(new BigDecimal("58000")),
            SettlementAmount.of(new BigDecimal("232000")),
            3L, 1L
        );
    }

    private Settlement confirmedSettlement(CreatorId creatorId, YearMonth yearMonth, LocalDateTime confirmedAt) {
        return Settlement.confirmedSnapshot(
            creatorId, yearMonth,
            Money.of(new BigDecimal("300000")),
            Money.of(new BigDecimal("10000")),
            SettlementAmount.of(new BigDecimal("290000")),
            FeeRate.of(new BigDecimal("0.2000")),
            Money.of(new BigDecimal("58000")),
            SettlementAmount.of(new BigDecimal("232000")),
            3L, 1L,
            OccurredAt.of(confirmedAt)
        );
    }

    private Settlement paidSettlement(CreatorId creatorId, YearMonth yearMonth, LocalDateTime confirmedAt, LocalDateTime paidAt) {
        return Settlement.paidSnapshot(
            creatorId, yearMonth,
            Money.of(new BigDecimal("300000")),
            Money.of(new BigDecimal("10000")),
            SettlementAmount.of(new BigDecimal("290000")),
            FeeRate.of(new BigDecimal("0.2000")),
            Money.of(new BigDecimal("58000")),
            SettlementAmount.of(new BigDecimal("232000")),
            3L, 1L,
            OccurredAt.of(confirmedAt),
            OccurredAt.of(paidAt)
        );
    }
}
