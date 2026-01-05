package com.example.navigatorrag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService implements CommandLineRunner {
    private final Logger log =  LoggerFactory.getLogger(IngestionService.class);
    private final MarkdownHeaderSplitter markdownSplitter;
    private final VectorService vectorService;
    private final JdbcTemplate jdbcTemplate;

    public IngestionService(MarkdownHeaderSplitter markdownSplitter, VectorService vectorService,  JdbcTemplate jdbcTemplate) {
        this.markdownSplitter = markdownSplitter;
        this.vectorService = vectorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Ingestion service started");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:file_data/*");

        for (Resource resource : resources) {
            String fileName = resource.getFilename();

            if(isAlreadyIngested(fileName)) {
                log.info("File already exists in our database: " + fileName);
                continue;
            }

            log.info("Ingesting file: " + fileName);
            processAndStore(resource);
        }

        log.info("Ingestion service finished");
    }

    private boolean isAlreadyIngested(String fileName) {
        try{
            String sql = "SELECT COUNT(*) FROM vector_store WHERE metadata->>'file_name' = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);
            return count != null && count > 0;
        } catch (Exception e){
            return false;
        }
    }

    private void processAndStore(Resource resource) {
        try {
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

            vectorService.storeChunks(chunks);
            log.info("Successfully stored {} chunks for file: {}", chunks.size(), filename);
        } catch (Exception e) {
            log.error("Failed to process file {}: {}", resource.getFilename(), e.getMessage());
        }
    }
}
