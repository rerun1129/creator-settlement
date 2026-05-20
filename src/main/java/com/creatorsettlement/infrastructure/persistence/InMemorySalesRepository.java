package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.repository.SalesRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemorySalesRepository implements SalesRepository {

    private final List<SalesRecord> sales = new CopyOnWriteArrayList<>();

    @Override
    public void save(SalesRecord salesRecord) {
        sales.add(salesRecord);
    }

    public List<SalesRecord> findAll() {
        return List.copyOf(sales);
    }
}
