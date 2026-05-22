package com.creatorsettlement.application.sales;

import java.util.List;

public interface SalesService {

    void register(RegisterSaleCommand command);

    void registerCancellation(RegisterCancellationCommand command);

    List<SalesListItem> listSales(ListSalesQuery query);
}
