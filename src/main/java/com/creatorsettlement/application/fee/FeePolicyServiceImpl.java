package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;

import java.time.LocalDate;
import java.util.List;

public class FeePolicyServiceImpl implements FeePolicyService {

    private final FeePolicyRepository repository;

    public FeePolicyServiceImpl(FeePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public FeeRate findEffectiveRate(LocalDate referenceDate) {
        LocalDate firstDayOfMonth = referenceDate.withDayOfMonth(1);
        return repository.findEffectiveAt(firstDayOfMonth)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.FEE_POLICY_NOT_FOUND_FOR_DATE.message()))
                .rate();
    }

    @Override
    public void register(RegisterFeePolicyCommand cmd) {
        FeePolicy policy = FeePolicy.of(FeeRate.of(cmd.rate()), cmd.effectiveFrom());
        repository.save(policy);
    }

    @Override
    public List<FeePolicyView> listAll() {
        return repository.findAllOrderByEffectiveFromDesc().stream()
                .map(FeePolicyView::from)
                .toList();
    }
}
