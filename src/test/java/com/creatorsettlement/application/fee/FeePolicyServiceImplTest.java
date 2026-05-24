package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("FeePolicyService 단위 테스트")
class FeePolicyServiceImplTest {

    private InMemoryFeePolicyRepository repository;
    private FeePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFeePolicyRepository();
        service = new FeePolicyServiceImpl(repository);
    }

    @Test
    @DisplayName("여러 정책 등록 시 기준일 이전 가장 최근 정책 반환")
    void findEffectiveRate_returnsLatestPolicy_whenMultiplePoliciesEffective() {
        // Given
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 7, 1)));

        // When
        FeeRate result = service.findEffectiveRate(LocalDate.of(2026, 8, 1));

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("0.18"));
    }

    @Test
    @DisplayName("기준일 이전 정책이 없으면 예외")
    void findEffectiveRate_throws_whenNoPolicyEffective() {
        // Given
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2026, 7, 1)));

        // When & Then
        assertThatThrownBy(() -> service.findEffectiveRate(LocalDate.of(2026, 6, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("기준일과 effectiveFrom이 동일하면 해당 정책 포함")
    void findEffectiveRate_returnsExactDatePolicy_whenReferenceMatchesEffectiveFrom() {
        // Given
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2026, 7, 1)));

        // When
        FeeRate result = service.findEffectiveRate(LocalDate.of(2026, 7, 1));

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("0.2"));
    }

    @Test
    @DisplayName("정상 등록 후 findEffectiveRate로 조회 가능")
    void register_storesPolicy_whenValid() {
        // Given
        RegisterFeePolicyCommand cmd = new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 7, 1));

        // When
        service.register(cmd);

        // Then
        FeeRate result = service.findEffectiveRate(LocalDate.of(2026, 8, 1));
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("0.18"));
    }

    @Test
    @DisplayName("동일 effectiveFrom 중복 등록 시 예외")
    void register_throws_whenDuplicateEffectiveFrom() {
        // Given
        RegisterFeePolicyCommand cmd = new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2026, 7, 1));
        service.register(cmd);

        // When & Then
        assertThatThrownBy(() -> service.register(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("등록된 정책 모두 반환 (정렬 검증 무관)")
    void listAll_returnsAllRegisteredPolicies() {
        // Given
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 1, 1)));
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 7, 1)));

        // When
        List<FeePolicyView> views = service.listAll();

        // Then
        assertThat(views)
                .extracting(view -> view.rate().stripTrailingZeros(), FeePolicyView::effectiveFrom)
                .containsExactlyInAnyOrder(
                        tuple(new BigDecimal("0.2").stripTrailingZeros(), LocalDate.of(2020, 1, 1)),
                        tuple(new BigDecimal("0.18").stripTrailingZeros(), LocalDate.of(2026, 7, 1))
                );
    }

    @Test
    @DisplayName("월 중간 날짜를 넘겨도 해당 월 1일 기준 정책 반환")
    void findEffectiveRate_normalizesToFirstDayOfMonth_whenMidMonthReferenceDate() {
        // Given
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 7, 1)));
        service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2026, 8, 15)));

        // When
        FeeRate rate = service.findEffectiveRate(LocalDate.of(2026, 8, 20));

        // Then
        assertThat(rate.value()).isEqualByComparingTo(new BigDecimal("0.18"));
    }
}
