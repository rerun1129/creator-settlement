package com.creatorsettlement.presentation.settlement;

import com.creatorsettlement.application.settlement.SettlementService;
import com.creatorsettlement.presentation.settlement.dto.GetMonthlySettlementRequest;
import com.creatorsettlement.presentation.settlement.dto.MonthlySettlementResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public ResponseEntity<MonthlySettlementResponse> getMonthlySettlement(
            @Valid @ModelAttribute GetMonthlySettlementRequest request) {
        return ResponseEntity.ok(MonthlySettlementResponse.from(
                settlementService.getMonthlySettlement(request.toQuery())));
    }
}
