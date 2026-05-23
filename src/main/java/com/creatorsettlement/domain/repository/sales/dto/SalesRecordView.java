package com.creatorsettlement.domain.repository.sales.dto;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

import java.util.List;

public record SalesRecordView(SalesRecordId id, SalesRecord record, List<CancellationRecord> cancellations, CreatorId creatorId) {}
