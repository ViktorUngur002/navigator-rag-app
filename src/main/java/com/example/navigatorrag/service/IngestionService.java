package com.example.navigatorrag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class IngestionService implements CommandLineRunner {
    private final Logger log =  LoggerFactory.getLogger(IngestionService.class);
    private final MarkdownHeaderSplitter markdownSplitter;
    private final VectorStore consultantTable;
    private final VectorStore clientTable;
    private final JdbcTemplate jdbcTemplate;

    public IngestionService(MarkdownHeaderSplitter markdownSplitter, @Qualifier("ConsultantDatabase") VectorStore consultantTable, @Qualifier("ClientDatabase") VectorStore clientTable, JdbcTemplate jdbcTemplate) {
        this.markdownSplitter = markdownSplitter;
        this.consultantTable = consultantTable;
        this.clientTable = clientTable;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Ingestion service started");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resourcesConsultant = resolver.getResources("classpath:resources_consultant/*");
        Resource[] resourcesClient = resolver.getResources("classpath:resources_client/*");

        this.processAndStoreChunks(resourcesConsultant, "consultant_resources");
        this.processAndStoreChunks(resourcesClient, "client_resources");

        log.info("Ingestion service finished");
    }


    private void processAndStoreChunks(Resource[] resources, String tableName) {
        for (Resource resource : resources) {
            String fileName = resource.getFilename();

            if(isAlreadyIngested(fileName, tableName)) {
                log.info("File already exists in our database: " + fileName);
                continue;
            }

            log.info("Ingesting file: " + fileName);
            store(resource);
        }
    }

    // Proverava da li dokument vec postoji u bazi
    private boolean isAlreadyIngested(String fileName, String tableName) {

        Set<String> allowedTables = Set.of("consultant_resources", "client_resources");

        if (!allowedTables.contains(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        String sql = "SELECT COUNT(*) FROM " + tableName +
                " WHERE metadata->>'file_name' = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);

        return count != null && count > 0;
    }

    // Creates chunks based on their file type
    private List<Document> createChunksBasedOnFileType(Resource resource) {
        List<Document> documents;
        List<Document> chunks;
        String filename = resource.getFilename();

        if (filename.toLowerCase().endsWith(".pdf")) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            documents = pdfReader.get();
            TokenTextSplitter splitter = new TokenTextSplitter(1000, 350, 5, 10000, true);
            chunks = splitter.apply(documents);
        } else {
            TextReader textReader = new TextReader(resource);
            documents = textReader.get();
            chunks = markdownSplitter.splitByH2(documents, filename);
        }

        return chunks;
    }

    // Stores the produced chunk in databse
    private void store(Resource resource) {
        try {
            String filename = resource.getFilename();
            List<Document> createdChunks;
            createdChunks = createChunksBasedOnFileType(resource);

           if(filename.toLowerCase().endsWith(".txt")) {
               consultantTable.add(createdChunks);
           } else if (filename.toLowerCase().endsWith(".pdf")) {
               consultantTable.add(createdChunks);
               clientTable.add(createdChunks);
           }

            log.info("Successfully stored {} chunks for file: {}", createdChunks.size(), filename);
        } catch (Exception e) {
            log.error("Failed to process file {}: {}", resource.getFilename(), e.getMessage());
        }
    }
}
