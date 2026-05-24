package com.creatorsettlement.presentation.settlement;

import com.creatorsettlement.application.settlement.SettlementService;
import com.creatorsettlement.application.settlement.dto.SettlementExcelDownload;
import com.creatorsettlement.presentation.settlement.dto.ConfirmSettlementRequest;
import com.creatorsettlement.presentation.settlement.dto.GetMonthlySettlementRequest;
import com.creatorsettlement.presentation.settlement.dto.GetSettlementRangeRequest;
import com.creatorsettlement.presentation.settlement.dto.MonthlySettlementResponse;
import com.creatorsettlement.presentation.settlement.dto.PaySettlementRequest;
import com.creatorsettlement.presentation.settlement.dto.SettlementRangeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/aggregate")
    public ResponseEntity<SettlementRangeResponse> getSettlementsInRange(
            @Valid @ModelAttribute GetSettlementRangeRequest request) {
        return ResponseEntity.ok(SettlementRangeResponse.from(
                settlementService.getSettlementsInRange(request.toQuery())));
    }

    @GetMapping("/aggregate/download")
    public ResponseEntity<byte[]> downloadSettlementsInRange(
            @Valid @ModelAttribute GetSettlementRangeRequest request) {
        SettlementExcelDownload download = settlementService.exportSettlementsInRangeAsExcel(request.toQuery());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, download.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, download.contentDisposition())
                .body(download.body());
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmSettlementRequest request) {
        settlementService.confirm(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/pay")
    public ResponseEntity<Void> pay(@Valid @RequestBody PaySettlementRequest request) {
        settlementService.pay(request.toCommand());
        return ResponseEntity.noContent().build();
    }
}
