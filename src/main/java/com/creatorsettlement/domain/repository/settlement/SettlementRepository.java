package com.creatorsettlement.domain.repository.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository {

    Optional<Settlement> findByCreatorIdAndYearMonth(CreatorId creatorId, YearMonth yearMonth);

    void save(Settlement settlement);

    List<Settlement> findByYearMonthRange(YearMonth fromInclusive, YearMonth toInclusive);
}
