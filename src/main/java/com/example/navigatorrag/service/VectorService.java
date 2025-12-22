package com.example.navigatorrag.service;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;

import java.util.List;

@Service
public class VectorService {
    private final VectorStore vectorStore;

    public VectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeChunks(List<Document> chunks) {
        vectorStore.add(chunks);
        System.out.println(">>> Successfully persisted " + chunks.size() + " chunks to PGVector.");
    }
}
