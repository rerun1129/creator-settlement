package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.infrastructure.persistence.InMemoryFeePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("FeePolicyService 단위 테스트")
class FeePolicyServiceImplTest {

    @Nested
    @DisplayName("effective rate 조회")
    class FindEffectiveRate {

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
        @DisplayName("기준일 이전 정책이 없으면 기본 수수료율(20%) 반환")
        void findEffectiveRate_returnsDefaultRate_whenNoPolicyEffective() {
            // Given
            service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2026, 7, 1)));

            // When
            FeeRate rate = service.findEffectiveRate(LocalDate.of(2026, 6, 1));

            // Then
            assertThat(rate.value()).isEqualByComparingTo(FeeRate.defaultRate().value());
        }

        @Test
        @DisplayName("기준일이 effectiveFrom과 같은 월이면 정책 미적용(다음 달부터 적용)")
        void findEffectiveRate_doesNotApplyPolicy_whenReferenceIsSameMonthAsEffectiveFrom() {
            // Given
            service.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2026, 7, 1)));

            // When
            FeeRate result = service.findEffectiveRate(LocalDate.of(2026, 7, 31));

            // Then
            assertThat(result.value()).isEqualByComparingTo(FeeRate.defaultRate().value());
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

        @Test
        @DisplayName("같은 월 중간에 등록된 후속 정책은 본 월에 미반영되고 다음 달 1일부터 적용된다")
        void findEffectiveRate_appliesUpdatedPolicy_fromNextMonth_whenRegisteredMidMonth() {
            // Given
            service.register(new RegisterFeePolicyCommand(new BigDecimal("0.2"), LocalDate.of(2020, 5, 1)));
            service.register(new RegisterFeePolicyCommand(new BigDecimal("0.18"), LocalDate.of(2020, 6, 15)));

            // When
            FeeRate currentMonth = service.findEffectiveRate(LocalDate.of(2020, 6, 20));
            FeeRate nextMonth = service.findEffectiveRate(LocalDate.of(2020, 7, 1));

            // Then
            assertThat(currentMonth.value()).isEqualByComparingTo(new BigDecimal("0.2"));
            assertThat(nextMonth.value()).isEqualByComparingTo(new BigDecimal("0.18"));
        }
    }

    @Nested
    @DisplayName("정책 등록")
    class Register {

        private InMemoryFeePolicyRepository repository;
        private FeePolicyServiceImpl service;

        @BeforeEach
        void setUp() {
            repository = new InMemoryFeePolicyRepository();
            service = new FeePolicyServiceImpl(repository);
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
    }

    @Nested
    @DisplayName("전체 조회")
    class ListAll {

        private InMemoryFeePolicyRepository repository;
        private FeePolicyServiceImpl service;

        @BeforeEach
        void setUp() {
            repository = new InMemoryFeePolicyRepository();
            service = new FeePolicyServiceImpl(repository);
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
    }
}
