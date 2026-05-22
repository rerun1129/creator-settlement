package com.creatorsettlement.presentation.sales;

import com.creatorsettlement.application.sales.SalesService;
import com.creatorsettlement.presentation.sales.dto.ListSalesRequest;
import com.creatorsettlement.presentation.sales.dto.RegisterCancellationRequest;
import com.creatorsettlement.presentation.sales.dto.RegisterSaleRequest;
import com.creatorsettlement.presentation.sales.dto.SalesItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    public ResponseEntity<Void> registerSale(@Valid @RequestBody RegisterSaleRequest req) {
        salesService.register(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/cancellations")
    public ResponseEntity<Void> registerCancellation(@Valid @RequestBody RegisterCancellationRequest req) {
        salesService.registerCancellation(req.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<SalesItemResponse>> listSales(@Valid @ModelAttribute ListSalesRequest req) {
        return ResponseEntity.ok(SalesItemResponse.fromAllSortedByPaidAtDesc(
                salesService.listSales(req.toQuery())));
    }
}
