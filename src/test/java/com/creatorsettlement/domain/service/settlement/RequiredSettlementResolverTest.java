package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.settlement.SettlementStatus;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import com.creatorsettlement.infrastructure.persistence.InMemorySettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("저장된 월 정산 조회 도메인 서비스 단위 테스트")
class RequiredSettlementResolverTest {

    private InMemorySettlementRepository settlementRepository;
    private RequiredSettlementResolver resolver;

    @BeforeEach
    void setUp() {
        settlementRepository = new InMemorySettlementRepository();
        resolver = new RequiredSettlementResolver(settlementRepository);
    }

    @Test
    @DisplayName("저장된 Settlement가 있으면 그 Settlement를 반환한다")
    void resolve_returns_stored_settlement_when_exists() {
        // Given
        CreatorId creatorId = CreatorId.of(90L);
        YearMonth yearMonth = YearMonth.of(2026, 4);
        Settlement stored = pendingFixture(creatorId, yearMonth);
        settlementRepository.save(stored);

        // When
        Settlement result = resolver.resolve(creatorId, yearMonth);

        // Then
        assertThat(result.creatorId()).isEqualTo(creatorId);
        assertThat(result.yearMonth()).isEqualTo(yearMonth);
        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.totalSales().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("100000"));
        assertThat(result.totalRefund().value()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("10000"));
        assertThat(result.salesCount()).isEqualTo(5L);
        assertThat(result.cancellationCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("저장된 Settlement가 없으면 예외가 발생한다")
    void resolve_throws_SETTLEMENT_NOT_FOUND_when_not_stored() {
        // Given
        CreatorId creatorId = CreatorId.of(91L);
        YearMonth yearMonth = YearMonth.of(2026, 4);

        // When & Then
        assertThatThrownBy(() -> resolver.resolve(creatorId, yearMonth))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("정산 내역을 찾을 수 없습니다");
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
}
