package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySettlementRepository implements SettlementRepository {

    private record Key(CreatorId creatorId, YearMonth yearMonth) {}

    private final Map<Key, Settlement> store = new ConcurrentHashMap<>();

    @Override
    public void save(Settlement settlement) {
        store.put(new Key(settlement.creatorId(), settlement.yearMonth()), settlement);
    }

    @Override
    public Optional<Settlement> findByCreatorIdAndYearMonth(CreatorId creatorId, YearMonth yearMonth) {
        return Optional.ofNullable(store.get(new Key(creatorId, yearMonth)));
    }

    @Override
    public List<Settlement> findByYearMonthRange(YearMonth fromInclusive, YearMonth toInclusive) {
        throw new UnsupportedOperationException("단계 3 어플리케이션 GREEN에서 구현");
    }
}
