package com.creatorsettlement.domain.model.settlement;

import com.creatorsettlement.domain.error.DomainErrorMessage;

public enum SettlementStatus {
    PENDING {
        @Override public SettlementStatus toConfirmed() { return CONFIRMED; }
        @Override public SettlementStatus toPaid() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_CONFIRMED_FOR_PAYMENT.message());
        }
    },
    CONFIRMED {
        @Override public SettlementStatus toConfirmed() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_CONFIRMED.message());
        }
        @Override public SettlementStatus toPaid() { return PAID; }
    },
    PAID {
        @Override public SettlementStatus toConfirmed() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
        @Override public SettlementStatus toPaid() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
    };

    public abstract SettlementStatus toConfirmed();
    public abstract SettlementStatus toPaid();
}
