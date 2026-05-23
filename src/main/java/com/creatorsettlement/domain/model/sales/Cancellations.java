package com.creatorsettlement.domain.model.sales;

import com.creatorsettlement.domain.model.vo.Money;

import java.math.BigDecimal;
import java.util.List;

public class Cancellations {

    private final List<CancellationRecord> records;

    public static Cancellations of(List<CancellationRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("cancellation records must not be null");
        }
        return new Cancellations(records);
    }

    private Cancellations(List<CancellationRecord> records) {
        this.records = List.copyOf(records);
    }

    public Money total() {
        return records.stream()
                .map(CancellationRecord::getRefundAmount)
                .reduce(Money.of(BigDecimal.ZERO), (a, b) -> Money.of(a.value().add(b.value())));
    }

    public Money remainingOf(Money paymentAmount) {
        return Money.of(paymentAmount.value().subtract(total().value()));
    }

    public boolean coversFully(Money paymentAmount) {
        return total().value().compareTo(paymentAmount.value()) >= 0;
    }
}
