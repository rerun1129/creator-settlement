package com.creatorsettlement.infrastructure.persistence.fee;

import com.creatorsettlement.infrastructure.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "fee_policy",
    uniqueConstraints = @UniqueConstraint(name = "uk_fee_policy_effective_from", columnNames = {"effective_from"})
)
public class FeePolicyJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_policy_id")
    private Long id;

    @Column(name = "rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    protected FeePolicyJpaEntity() {}

    static FeePolicyJpaEntity of(BigDecimal rate, LocalDate effectiveFrom) {
        FeePolicyJpaEntity e = new FeePolicyJpaEntity();
        e.rate = rate;
        e.effectiveFrom = effectiveFrom;
        return e;
    }

    Long getId() { return id; }
    BigDecimal getRate() { return rate; }
    LocalDate getEffectiveFrom() { return effectiveFrom; }
}
