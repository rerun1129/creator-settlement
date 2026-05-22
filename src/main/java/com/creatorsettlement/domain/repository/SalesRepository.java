package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesRepository {

    void saveSalesRecord(SalesRecord salesRecord);

    Optional<SalesRecord> findById(SalesRecordId salesRecordId);

    Money sumRefundsBySalesRecordId(SalesRecordId salesRecordId);

    void saveCancellationRecord(CancellationRecord cancellationRecord);

    List<SalesRecordView> findSalesView(Optional<CreatorId> creatorId, LocalDateTime from, LocalDateTime toExclusive);
}
