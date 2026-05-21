package com.creatorsettlement.application;

import java.time.LocalDateTime;

public record ListSalesQuery(Long creatorId, LocalDateTime from, LocalDateTime toExclusive) {}
