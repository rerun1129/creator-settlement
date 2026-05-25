package com.creatorsettlement.application.fee;

import com.creatorsettlement.application.fee.dto.FeePolicyView;
import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;

import java.util.List;

public interface FeePolicyService {

    void register(RegisterFeePolicyCommand cmd);

    List<FeePolicyView> listAll();
}
