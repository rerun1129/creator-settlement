package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import com.creatorsettlement.application.settlement.dto.SettlementExcelDownload;
import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.domain.service.settlement.PendingSettlementResolver;
import com.creatorsettlement.domain.service.settlement.RequiredSettlementResolver;
import com.creatorsettlement.domain.service.settlement.SettlementRangePayoutAssembler;
import com.creatorsettlement.domain.service.settlement.SettlementMonthClosurePolicy;
import com.creatorsettlement.domain.service.settlement.SettlementRangePayoutResult;
import com.creatorsettlement.infrastructure.settlement.excel.SettlementExcelWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementExcelWriter settlementExcelWriter;
    private final SettlementMonthClosurePolicy monthClosurePolicy;
    private final PendingSettlementResolver pendingSettlementResolver;
    private final RequiredSettlementResolver requiredSettlementResolver;
    private final SettlementRangePayoutAssembler settlementRangePayoutAssembler;

    public SettlementServiceImpl(
            SettlementRepository settlementRepository,
            SettlementExcelWriter settlementExcelWriter,
            SettlementMonthClosurePolicy monthClosurePolicy,
            PendingSettlementResolver pendingSettlementResolver,
            RequiredSettlementResolver requiredSettlementResolver,
            SettlementRangePayoutAssembler settlementRangePayoutAssembler
    ) {
        this.settlementRepository = settlementRepository;
        this.settlementExcelWriter = settlementExcelWriter;
        this.monthClosurePolicy = monthClosurePolicy;
        this.pendingSettlementResolver = pendingSettlementResolver;
        this.requiredSettlementResolver = requiredSettlementResolver;
        this.settlementRangePayoutAssembler = settlementRangePayoutAssembler;
    }

    @Override
    public MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query) {
        CreatorId creatorId = CreatorId.of(query.creatorId());
        YearMonth requestedYearMonth = query.yearMonth();
        Settlement settlement = settlementRepository
                .findByCreatorIdAndYearMonth(creatorId, requestedYearMonth)
                .orElseGet(() -> pendingSettlementResolver.resolve(creatorId, requestedYearMonth));
        return MonthlySettlementView.from(settlement);
    }

    @Override
    @Transactional
    public void confirm(ConfirmSettlementCommand command) {
        monthClosurePolicy.verifyMonthClosed(command.yearMonth());
        CreatorId creatorId = CreatorId.of(command.creatorId());
        YearMonth requestedYearMonth = command.yearMonth();
        Settlement settlement = settlementRepository
                .findByCreatorIdAndYearMonth(creatorId, requestedYearMonth)
                .orElseGet(() -> pendingSettlementResolver.resolve(creatorId, requestedYearMonth));
        settlement.confirm(OccurredAt.of(command.confirmedAt()));
        settlementRepository.save(settlement);
    }

    @Override
    @Transactional
    public void pay(PaySettlementCommand command) {
        monthClosurePolicy.verifyMonthClosed(command.yearMonth());
        Settlement settlement = requiredSettlementResolver.resolve(CreatorId.of(command.creatorId()), command.yearMonth());
        settlement.pay(OccurredAt.of(command.paidAt()));
        settlementRepository.save(settlement);
    }

    @Override
    public SettlementRangeView getSettlementsInRange(SettlementRangeQuery query) {
        SettlementRangePayoutResult result = settlementRangePayoutAssembler.assemble(query.from(), query.to());
        List<CreatorPayableView> responses = result.payouts().stream()
                .map(payout -> new CreatorPayableView(payout.creatorId().value(), payout.expectedPayout()))
                .toList();
        return new SettlementRangeView(responses, result.totalAmount());
    }

    @Override
    public SettlementExcelDownload exportSettlementsInRangeAsExcel(SettlementRangeQuery query) {
        return settlementExcelWriter.write(getSettlementsInRange(query), query.from(), query.to());
    }
}
