package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;

public interface SettlementService {

    MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query);

    void confirm(ConfirmSettlementCommand command);

    void pay(PaySettlementCommand command);

    SettlementRangeView getSettlementsInRange(SettlementRangeQuery query);
}
