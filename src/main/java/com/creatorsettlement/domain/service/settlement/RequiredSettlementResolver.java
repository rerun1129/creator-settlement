package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class RequiredSettlementResolver {

    private final SettlementRepository settlementRepository;

    public RequiredSettlementResolver(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    public Settlement resolve(CreatorId creatorId, YearMonth yearMonth) {
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_FOUND.message()));
    }
}
