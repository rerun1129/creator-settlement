package com.creatorsettlement.infrastructure.persistence.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.sales.SalesRecordView;

import java.util.List;
import java.util.Map;

class SalesRecordViewAssembler {

    private SalesRecordViewAssembler() {
    }

    static List<SalesRecordView> assemble(
            List<SalesRecordJpaEntity> sales,
            Map<Long, List<CancellationRecordJpaEntity>> cancelByRecordId
    ) {
        return sales.stream()
                .map(entity -> {
                    SalesRecordId salesRecordId = SalesRecordId.of(entity.getId());
                    List<CancellationRecord> cancellations = cancelByRecordId
                            .getOrDefault(entity.getId(), List.of())
                            .stream()
                            .map(SalesRecordMapper::toDomainCancellation)
                            .toList();
                    CreatorId creatorId = CreatorId.of(entity.getCourse().getCreatorId());
                    return new SalesRecordView(salesRecordId, SalesRecordMapper.toDomainSalesRecord(entity), cancellations, creatorId);
                })
                .toList();
    }
}
