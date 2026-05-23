package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;

public interface SettlementService {

    MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query);

    void confirm(ConfirmSettlementCommand command);

    void pay(PaySettlementCommand command);
}
