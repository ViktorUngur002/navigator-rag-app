package com.example.navigatorrag.controller;

import com.example.navigatorrag.service.AuditLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// Metrics endpoint
@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {
    private final AuditLogService auditLogService;
    @Value("${app.ai.audit.similarity.threshold}")
    private double threshold;

    public MetricsController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/unanswered")
    public Map<String, Double> topUnanswered() {
        return auditLogService.unansweredBySimilarityScore(threshold);
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
                auditLogService.averageTokenUsePerSession()
        );

        return result;
    }
}
