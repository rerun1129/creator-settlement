package com.creatorsettlement.domain.service.fee;

import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeePolicyDomainService 단위 테스트")
class FeePolicyDomainServiceTest {

    private InMemoryFeePolicyRepository repository;
    private FeePolicyDomainService domainService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFeePolicyRepository();
        domainService = new FeePolicyDomainService(repository);
    }

    @Test
    @DisplayName("동일 effectiveFrom 정책이 이미 존재하면 예외가 발생한다")
    void ensureUniqueEffectiveFrom_throws_whenDuplicateExists() {
        // given
        LocalDate effectiveFrom = LocalDate.of(2026, 7, 1);
        repository.save(FeePolicy.of(FeeRate.of(new BigDecimal("0.18")), effectiveFrom));

        // when & then
        assertThatThrownBy(() -> domainService.ensureUniqueEffectiveFrom(effectiveFrom))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 저장소에서는 예외 없이 통과한다")
    void ensureUniqueEffectiveFrom_passes_whenNoDuplicate() {
        // given
        LocalDate effectiveFrom = LocalDate.of(2026, 7, 1);

        // when & then
        assertThatCode(() -> domainService.ensureUniqueEffectiveFrom(effectiveFrom))
                .doesNotThrowAnyException();
    }
}
