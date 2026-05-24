package com.creatorsettlement.presentation.fee;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.presentation.fee.dto.FeePolicyResponse;
import com.creatorsettlement.presentation.fee.dto.RegisterFeePolicyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fee-policies")
public class FeePolicyController {

    private final FeePolicyService feePolicyService;

    public FeePolicyController(FeePolicyService feePolicyService) {
        this.feePolicyService = feePolicyService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterFeePolicyRequest request) {
        feePolicyService.register(request.toCommand());
        return ResponseEntity.created(URI.create("/api/fee-policies")).build();
    }

    @GetMapping
    public ResponseEntity<List<FeePolicyResponse>> listAll() {
        List<FeePolicyResponse> responses = feePolicyService.listAll().stream()
                .map(FeePolicyResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
