package com.example.navigatorrag.controller;

import com.example.navigatorrag.service.MarkdownHeaderSplitter;
import com.example.navigatorrag.service.VectorService;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ai.document.Document;

import java.util.List;

@Controller
public class FileController {
    // UPLOAD_DIRECTORY removed - we no longer save to disk
    private final MarkdownHeaderSplitter markdownSplitter;
    private final VectorService vectorService;

    public FileController(MarkdownHeaderSplitter markdownSplitter, VectorService vectorService) {
        this.markdownSplitter = markdownSplitter;
        this.vectorService = vectorService;
    }

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes attributes) {
        if (file.isEmpty()) {
            attributes.addFlashAttribute("message", "File is empty");
            return "redirect:/";
        }

        try {
            Resource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            List<Document> documents;
            List<Document> chunks;

            if (file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                documents = pdfReader.get();
                // 1000 tokens per chunk for the manual
                TokenTextSplitter splitter = new TokenTextSplitter(1000, 350, 5, 10000, true);
                chunks = splitter.apply(documents);
            } else {
                TextReader textReader = new TextReader(resource);
                documents = textReader.get();
                chunks = markdownSplitter.splitByH2(documents, file.getOriginalFilename());
            }

            System.out.println("\n========================================================");
            System.out.println("PROCESSING FILE: " + file.getOriginalFilename());
            System.out.println("TOTAL CHUNKS CREATED: " + chunks.size());
            System.out.println("========================================================\n");

            for (int i = 0; i < chunks.size(); i++) {
                System.out.println("--- START CHUNK #" + (i + 1) + " ---");
                System.out.println("METADATA: " + chunks.get(i).getMetadata());
                System.out.println("CONTENT: " + chunks.get(i).getContent());
                System.out.println("--- END CHUNK #" + (i + 1) + " ---\n");
            }

            System.out.println("=================== END OF FILE ========================\n");

            vectorService.storeChunks(chunks);

            attributes.addFlashAttribute("message", "Successfully stored in Vector DB: " + file.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
            attributes.addFlashAttribute("message", "Error processing file: " + e.getMessage());
        }

        return "redirect:/";
    }
}