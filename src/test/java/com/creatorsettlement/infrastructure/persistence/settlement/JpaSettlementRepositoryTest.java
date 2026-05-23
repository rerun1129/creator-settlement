package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.infrastructure.config.JpaAuditingConfig;
import com.creatorsettlement.infrastructure.persistence.creator.CreatorJpaEntity;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("동일 creator·yearMonth로 두 번 저장하면 unique 제약 위반 예외가 발생한다")
    void unique_constraint_on_creator_and_yearmonth() {
        // given
        Long dbCreatorId = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 D")).getId();
        CreatorId creatorId = CreatorId.of(dbCreatorId);
        YearMonth yearMonth = YearMonth.of(2026, 5);

        sut.save(pendingSettlement(creatorId, yearMonth));
        em.flush();

        // when & then
        assertThatThrownBy(() -> {
            sut.save(pendingSettlement(creatorId, yearMonth));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
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
}
