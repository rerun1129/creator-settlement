package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.sale.SalesRecord;

public interface SalesRepository {

    void save(SalesRecord salesRecord);
}
