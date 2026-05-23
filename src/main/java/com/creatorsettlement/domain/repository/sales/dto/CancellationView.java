package com.creatorsettlement.domain.repository.sales.dto;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;

public record CancellationView(CancellationRecord record, CreatorId creatorId) {}
