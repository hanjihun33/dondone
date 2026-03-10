package com.workproofpay.backend.remittance.api;

import com.workproofpay.backend.shared.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/remittance")
public class RemittanceController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ApiResponse.success(Map.of("module", "remittance", "status", "skeleton-ready"));
    }
}
