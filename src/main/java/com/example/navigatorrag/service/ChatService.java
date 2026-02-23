package com.example.navigatorrag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
public class ChatService {
    public final ChatClient chatClient;
    public final AuditLogService auditLogService;
    private final ChatClient guardrailClient;
    public final VectorStore consultantTable;
    public final VectorStore clientTable;
    public final ChatMemory chatMemory;
    private String rag_prompt_template;

    public ChatService(ChatClient.Builder builder,
                       @Qualifier("ConsultantDatabase") VectorStore consultantTable,
                       @Qualifier("ClientDatabase") VectorStore clientTable,
                       AuditLogService auditLogService,
                       ChatMemory chatMemory,
                       @Value("${app.ai.main.model}") String mainModel,
                       @Value("${app.ai.main.temperature}") Double mainTemp,
                       @Value("${app.ai.guardrail.model}") String guardModel,
                       @Value("${app.ai.guardrail.temperature}") Double guardTemp) {

        this.consultantTable = consultantTable;
        this.clientTable = clientTable;
        this.auditLogService = auditLogService;
        this.chatMemory = chatMemory;
        this.rag_prompt_template = this.getPrompt("RAG_prompt_template.txt");

        // Glavni model koji odgovara na pitanja
        this.chatClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(mainModel)
                        .withTemperature(mainTemp)
                        .build())
                .build();

        // Guardrail model koji ne dozvoljava opsta pitanja usera
        this.guardrailClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(guardModel)
                        .withTemperature(guardTemp)
                        .build())
                .build();
    }

    // Metoda koja koristi glavni model da bi odgovorila na pitanje, koristeci kontekst iz baze podataka
    public String generateRespone(String sessionId, String role, String userMessage) {

        if(isOutOfScope(userMessage)){
            return "Ovo pitanje je izvan opsega aplikacije Business Navigator RAG.";
        }

        long startTime = System.currentTimeMillis();

        String systemPrompt = role.equalsIgnoreCase("CONSULTANT")
                ? this.getPrompt("consultant_system_prompt.txt")
                : this.getPrompt("client_system_prompt.txt");

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filter = role.equalsIgnoreCase("CLIENT")
                ? b.eq("file_name", "manual.pdf").build()
                : null;

        VectorStore currentlyUsedTable;
        if(role.equalsIgnoreCase("CONSULTANT")){
            currentlyUsedTable = consultantTable;
        } else {
            currentlyUsedTable = clientTable;
        }

        SearchRequest searchRequest = SearchRequest.defaults().withTopK(3).withFilterExpression(filter);

        var response = this.chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(new QuestionAnswerAdvisor(currentlyUsedTable, searchRequest, this.rag_prompt_template))
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .call()
                .chatResponse();

        long endTime = System.currentTimeMillis();
        double responseTime = ((endTime - startTime)/1000.0);

        long totalTokens = response.getMetadata().getUsage().getTotalTokens();

        List<Document> documents = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

        String modelResponse = response.getResult().getOutput().getContent();

        auditLogService.save(userMessage, modelResponse, documents, totalTokens, responseTime, role, sessionId);

        return modelResponse;
    }

    // Metoda koja dobavlja sacuvane promptove
    private String getPrompt(String fileName) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:prompts/" +  fileName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt file: " + fileName, e);
        }
    }

    // Metoda koja proverava da li je upit van opsega aplikacije
    private boolean isOutOfScope(String userMessage) {
        String template = this.getPrompt("scope_guardrail_prompt.txt");
        String classificationPrompt = template.formatted(userMessage);

        String result = this.guardrailClient.prompt()
                .user(classificationPrompt)
                .call()
                .content();

        return result != null && result.trim().toUpperCase().contains("VAN_TEME");
    }
}
