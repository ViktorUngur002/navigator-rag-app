package com.example.navigatorrag.entity;

import com.example.navigatorrag.repository.AuditLogRepository;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="rag_audit_log")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String query;
    @Column(columnDefinition = "TEXT")
    private String response;
    private double similarityScore;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "sources", columnDefinition = "text[]")
    private List<String> sources;
    private long tokenUsage;
    private double responseTime;
    private String userRole;
    private String sessionId;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(String query, String response, double similarityScore, List<String> sources, long tokenUsage, double responseTime, String userRole,  String sessionId) {
        this.query = query;
        this.response = response;
        this.similarityScore = similarityScore;
        this.sources = sources;
        this.tokenUsage = tokenUsage;
        this.responseTime = responseTime;
        this.userRole = userRole;
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public String getResponse() {
        return response;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public double getResponseTime() {
        return responseTime;
    }

    public List<String> getSources() {
        return sources;
    }

    public long getTokenUsage() {
        return tokenUsage;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public void setTokenUsage(long tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public void setResponseTime(double responseTime) {
        this.responseTime = responseTime;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
