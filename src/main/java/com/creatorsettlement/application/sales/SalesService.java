package com.creatorsettlement.application.sales;

import com.creatorsettlement.application.sales.dto.ListSalesQuery;
import com.creatorsettlement.application.sales.dto.RegisterCancellationCommand;
import com.creatorsettlement.application.sales.dto.RegisterSaleCommand;
import com.creatorsettlement.application.sales.dto.SalesListItem;

import java.util.List;

public interface SalesService {

    void register(RegisterSaleCommand command);

    void registerCancellation(RegisterCancellationCommand command);

    List<SalesListItem> listSales(ListSalesQuery query);
}
