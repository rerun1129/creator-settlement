package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemorySalesRepository implements SalesRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<SalesRecordId, SalesRecord> salesById = new ConcurrentHashMap<>();
    private final List<CancellationRecord> cancellations = new CopyOnWriteArrayList<>();

    @Override
    public void saveSalesRecord(SalesRecord salesRecord) {
        SalesRecordId id = SalesRecordId.of(sequence.incrementAndGet());
        salesById.put(id, salesRecord);
    }

    @Override
    public Optional<SalesRecord> findById(SalesRecordId salesRecordId) {
        return Optional.ofNullable(salesById.get(salesRecordId));
    }

    @Override
    public Money sumRefundsBySalesRecordId(SalesRecordId salesRecordId) {
        BigDecimal sum = cancellations.stream()
                .filter(c -> c.getSalesRecordId().equals(salesRecordId))
                .map(c -> c.getRefundAmount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(sum);
    }

    @Override
    public void saveCancellationRecord(CancellationRecord cancellationRecord) {
        cancellations.add(cancellationRecord);
    }

    public List<SalesRecord> findAll() {
        return new ArrayList<>(salesById.values());
    }
}
