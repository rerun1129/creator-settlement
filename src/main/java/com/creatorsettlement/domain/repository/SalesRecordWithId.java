package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

public record SalesRecordWithId(SalesRecordId id, SalesRecord record) {}
