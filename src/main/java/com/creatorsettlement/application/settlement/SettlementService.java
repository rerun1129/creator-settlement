package com.creatorsettlement.application.settlement;

public interface SettlementService {

    MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query);
}
