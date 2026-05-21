package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

import java.util.List;

public record SalesRecordView(SalesRecordId id, SalesRecord record, List<CancellationRecord> cancellations, CreatorId creatorId) {}
