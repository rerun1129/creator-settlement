package com.creatorsettlement.domain.repository.sales.dto;

import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

public record SalesRecordWithId(SalesRecordId id, SalesRecord record) {}
