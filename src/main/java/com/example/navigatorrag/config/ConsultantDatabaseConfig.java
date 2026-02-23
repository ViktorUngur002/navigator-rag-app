package com.example.navigatorrag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ConsultantDatabaseConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimensions}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.pgvector.distance-type}")
    private String distanceType;

    @Bean
    @Qualifier("ConsultantDatabase")
    public VectorStore consultantDatabase(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return new PgVectorStore.Builder(jdbcTemplate, embeddingModel)
                .withVectorTableName("consultant_resources")
                .withDimensions(dimensions)
                .withDistanceType(PgVectorStore.PgDistanceType.valueOf(distanceType))
                .withIndexType(PgVectorStore.PgIndexType.NONE)
                .withInitializeSchema(true)
                .build();
    }
}
