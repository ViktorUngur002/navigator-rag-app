package com.example.navigatorrag.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Path;

@Service
public class MarkdownHeaderSplitter {
    public List<Document> splitByH2(List<Document> documents, String filename) {
        List<Document> chunks = new ArrayList<>();

        for (Document doc : documents) {
            String content = doc.getContent();
            Pattern pattern = Pattern.compile("(?m)^h2\\.\\s");
            Matcher matcher = pattern.matcher(content);

            int lastIndex = 0;
            String h1Intro = "";
            boolean isFirstChunk = true;

            while (matcher.find()) {
                String chunkText = content.substring(lastIndex, matcher.start()).trim();

                if (isFirstChunk) {
                    h1Intro = chunkText;
                    isFirstChunk = false;
                } else if (!chunkText.isEmpty()) {
                    String finalContent = h1Intro.isEmpty() ? chunkText : h1Intro + "\n\n" + chunkText;
                    h1Intro = "";

                    chunks.add(new Document(finalContent, Map.of(
                            "source", filename,
                            "type", "wiki_section"
                    )));
                }
                lastIndex = matcher.start();
            }

            String finalChunk = content.substring(lastIndex).trim();
            if (!finalChunk.isEmpty()) {
                String finalContent = h1Intro.isEmpty() ? finalChunk : h1Intro + "\n\n" + finalChunk;
                chunks.add(new Document(finalContent, Map.of(
                        "source", filename,
                        "type", "wiki_section"
                )));
            }
        }
        return chunks;
    }
}