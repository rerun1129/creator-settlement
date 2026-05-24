package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

class SettlementMapper {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private SettlementMapper() {}

    static SettlementJpaEntity toEntity(Settlement d) {
        return SettlementJpaEntity.of(
            d.creatorId().value(),
            d.yearMonth().format(YEAR_MONTH_FORMAT),
            d.status(),
            d.totalSales().value(),
            d.totalRefund().value(),
            d.netSales().value(),
            d.feeRate().value(),
            d.platformFee().value(),
            d.expectedPayout().value(),
            d.salesCount(),
            d.cancellationCount(),
            d.confirmedAt() == null ? null : d.confirmedAt().value(),
            d.paidAt() == null ? null : d.paidAt().value()
        );
    }

    static Settlement toDomain(SettlementJpaEntity e) {
        CreatorId creatorId = CreatorId.of(e.getCreatorId());
        YearMonth ym = YearMonth.parse(e.getYearMonth(), YEAR_MONTH_FORMAT);
        Money totalSales = Money.of(e.getTotalSales());
        Money totalRefund = Money.of(e.getTotalRefund());
        SettlementAmount netSales = SettlementAmount.of(e.getNetSales());
        FeeRate feeRate = FeeRate.of(e.getFeeRate());
        Money platformFee = Money.of(e.getPlatformFee());
        SettlementAmount expectedPayout = SettlementAmount.of(e.getExpectedPayout());

        return e.getStatus().toSettlement(
                creatorId, ym, totalSales, totalRefund, netSales,
                feeRate, platformFee, expectedPayout,
                e.getSalesCount(), e.getCancellationCount(),
                e.getConfirmedAt() == null ? null : OccurredAt.of(e.getConfirmedAt()),
                e.getPaidAt() == null ? null : OccurredAt.of(e.getPaidAt())
        );
    }

    static void applyTo(SettlementJpaEntity entity, Settlement domain) {
        entity.applyStateTransition(
            domain.status(),
            domain.confirmedAt() == null ? null : domain.confirmedAt().value(),
            domain.paidAt() == null ? null : domain.paidAt().value()
        );
    }
}
