package com.example.navigatorrag.service;

import com.example.navigatorrag.entity.AuditLogEntity;
import com.example.navigatorrag.repository.AuditLogRepository;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Map<String, Double> unansweredBySimilarityScore(double treshold) {
        List<AuditLogEntity> logs = auditLogRepository.findBySimilarityScoreLessThan(treshold);

        Map<String, Double> result = new HashMap<>();

        for (AuditLogEntity log : logs) {
            result.put(log.getQuery(), log.getSimilarityScore());
        }

        return result;
    }

    public Map<String, Double> averageResponseTimePerSession() {

        List<AuditLogEntity> logs = auditLogRepository.findAll();

        Map<String, Double> sumPerSession = new HashMap<>();
        Map<String, Integer> countPerSession = new HashMap<>();

        for (AuditLogEntity log : logs) {
            String sessionId = log.getSessionId();

            sumPerSession.merge(sessionId, log.getResponseTime(), Double::sum);
            countPerSession.merge(sessionId, 1, Integer::sum);
        }

        Map<String, Double> result = new HashMap<>();

        for (String sessionId : sumPerSession.keySet()) {
            double average =
                    sumPerSession.get(sessionId) / countPerSession.get(sessionId);
            result.put(sessionId, average);
        }

        return result;
    }

    public Map<String, Double> averageTokenUse() {
        List<AuditLogEntity> logs = auditLogRepository.findAll();

        Map<String, Long> sumPerSession = new HashMap<>();
        Map<String, Integer> countPerSession = new HashMap<>();

        for (AuditLogEntity log : logs) {
            String sessionId = log.getSessionId();

            sumPerSession.merge(sessionId, log.getTokenUsage(), Long::sum);
            countPerSession.merge(sessionId, 1, Integer::sum);
        }

        Map<String, Double> result = new HashMap<>();

        for (String sessionId : sumPerSession.keySet()) {
            double average =
                    (double) sumPerSession.get(sessionId) / countPerSession.get(sessionId);
            result.put(sessionId, average);
        }

        return result;
    }

    @Async
    public void save(String query, String response, List<Document> documents, long tokenUsage, double responseTime, String userRole, String sessionId) {

        double maxSimilarity = 0.0;
        List<String> fileSources = new ArrayList<>();

        if (documents != null) {
            for (var doc : documents) {
                Object distanceObj = doc.getMetadata().get("distance");
                if (distanceObj instanceof Number distanceNum) {
                    double similarity = 1.0 - distanceNum.doubleValue();
                    if (similarity > maxSimilarity) maxSimilarity = similarity;
                }

                String fileName = (String) doc.getMetadata().get("file_name");
                if (fileName != null) fileSources.add(fileName);
            }
        }

        auditLogRepository.save(
                new AuditLogEntity(query, response, maxSimilarity, fileSources, tokenUsage, responseTime, userRole, sessionId)
        );
    }
}
