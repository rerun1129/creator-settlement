package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;
import com.creatorsettlement.domain.service.fee.FeePolicyDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeePolicyServiceImpl implements FeePolicyService {

    private final FeePolicyRepository repository;
    private final FeePolicyDomainService domainService;

    public FeePolicyServiceImpl(FeePolicyRepository repository, FeePolicyDomainService domainService) {
        this.repository = repository;
        this.domainService = domainService;
    }

    @Override
    public void register(RegisterFeePolicyCommand cmd) {
        domainService.ensureUniqueEffectiveFrom(cmd.effectiveFrom());
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
