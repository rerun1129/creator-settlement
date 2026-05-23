package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public class JpaSettlementRepository implements SettlementRepository {

    @Override
    public Optional<Settlement> findByCreatorIdAndYearMonth(CreatorId creatorId, YearMonth yearMonth) {
        return Optional.empty();
    }

    @Override
    public void save(Settlement settlement) {
        throw new UnsupportedOperationException("Phase 3 JPA 구현 예정");
    }
}
