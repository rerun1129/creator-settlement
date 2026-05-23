package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Repository
public class JpaSettlementRepository implements SettlementRepository {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final SettlementJpaDataRepository dataRepository;

    public JpaSettlementRepository(SettlementJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public Optional<Settlement> findByCreatorIdAndYearMonth(CreatorId creatorId, YearMonth yearMonth) {
        String ymString = yearMonth.format(YEAR_MONTH_FORMAT);
        return dataRepository.findByCreatorIdAndYearMonth(creatorId.value(), ymString)
                .map(SettlementMapper::toDomain);
    }

    @Override
    public void save(Settlement settlement) {
        dataRepository.save(SettlementMapper.toEntity(settlement));
    }
}
