package com.example.navigatorrag.controller;

import com.example.navigatorrag.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {
    private final AuditLogService auditLogService;

    public MetricsController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/unanswered")
    public Map<String, Double> topUnanswered() {
        return auditLogService.unansweredBySimilarityScore(0.5);
    }

    @GetMapping("/efficiency")
    public Map<String, Object> efficiencyReport() {

        Map<String, Object> result = new HashMap<>();

        result.put(
                "averageResponseTimePerSession",
                auditLogService.averageResponseTimePerSession()
        );

        result.put(
                "averageTokenUsagePerSession",
                auditLogService.averageTokenUse()
        );

        return result;
    }
}
