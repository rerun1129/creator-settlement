package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;

public interface SettlementService {

    MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query);
}
