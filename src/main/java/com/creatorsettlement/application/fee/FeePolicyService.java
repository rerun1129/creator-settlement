package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import com.creatorsettlement.domain.model.vo.FeeRate;

import java.time.LocalDate;
import java.util.List;

public interface FeePolicyService {

    FeeRate findEffectiveRate(LocalDate referenceDate);

    void register(RegisterFeePolicyCommand cmd);

    List<FeePolicyView> listAll();
}
