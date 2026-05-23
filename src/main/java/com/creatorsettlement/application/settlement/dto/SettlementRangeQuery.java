package com.creatorsettlement.application.settlement.dto;

import java.time.LocalDate;

public record SettlementRangeQuery(LocalDate from, LocalDate to) {
}
