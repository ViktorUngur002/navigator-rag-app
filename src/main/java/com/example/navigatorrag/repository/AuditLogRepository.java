package com.example.navigatorrag.repository;

import com.example.navigatorrag.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findBySimilarityScoreLessThan(double treshold);
}
