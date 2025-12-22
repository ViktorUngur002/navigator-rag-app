package com.example.navigatorrag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
public class ChatService {
    public final ChatClient chatClient;
    private final ChatClient guardrailClient;
    public final VectorStore vectorStore;
    public final ChatMemory chatMemory;
    private final String RAG_PROMPT_TEMPLATE = """
            Ti si asistent u sistemu Business Navigator. 
            Ispod se nalazi kontekst iz dokumentacije I prethodna istorija razgovora.
            Koristi oba izvora da odgovoriš na pitanje. 
            Ako se odgovor ne nalazi u dokumentaciji, ali ga znaš iz istorije, slobodno ga koristi.
            
            KONTEKST:
            {question_answer_context}
            """;

    public ChatService(ChatClient.Builder builder,
                       VectorStore vectorStore,
                       ChatMemory chatMemory,
                       @Value("${app.ai.main.model}") String mainModel,
                       @Value("${app.ai.main.temperature}") Double mainTemp,
                       @Value("${app.ai.guardrail.model}") String guardModel,
                       @Value("${app.ai.guardrail.temperature}") Double guardTemp) {

        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;

        // Main Client for RAG and Tools
        this.chatClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(mainModel)
                        .withTemperature(mainTemp)
                        .build())
                .build();

        // Lightweight Guardrail Client
        this.guardrailClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(guardModel)
                        .withTemperature(guardTemp)
                        .build())
                .build();
    }

    public String generateRespone(String sessionId, String role, String userMessage) {
        if(isOutOfScope(userMessage)){
            return "Ovo pitanje je izvan opsega aplikacije Business Navigator RAG.";
        }

        String systemPrompt = role.equalsIgnoreCase("CONSULTANT")
                ? "Ti si tehnički ekspert. Koristi dokumentaciju, alate za konverziju, ali i dodatne servisne parametre: [Podešavanja: Port 8080, DB_Timeout: 30s, Max_Users: 500]."
                : "Ti si ljubazni asistent. Koristi isključivo priloženu dokumentaciju i dostupne alate.";

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        var filter = role.equalsIgnoreCase("CLIENT")
                ? b.eq("file_name", "manual.pdf").build()
                : null;

        List<String> activeFunctions = new ArrayList<>();
        activeFunctions.add("convertCurrency");

        if(role.equalsIgnoreCase("CLIENT")) {
            activeFunctions.add("getSalesData");
        }

        SearchRequest searchRequest = SearchRequest.defaults().withTopK(3).withFilterExpression(filter);

        return this.chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .functions(activeFunctions.toArray(new String[0]))
                .advisors(new MessageChatMemoryAdvisor(chatMemory))
                .advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest, this.RAG_PROMPT_TEMPLATE))
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .call()
                .content();
    }

    private boolean isOutOfScope(String userMessage) {
        String classificationPrompt = """
                Ti si inteligentni filter za poslovnu aplikaciju Business Navigator RAG. 
                Tvoj zadatak je da procenis nameru korisnika.
                Odgovori ISKLJUČIVO sa „VAN_TEME“ ili „U_TEMI“.
                
                DOZVOLJENI UPITI:
                - Pitanja i upiti vezani za jedan ERP sistem, i sve njegove module.
                - Upiti o koriscenju softvera, modulima i podesavanjima.
                - Upiti o klijentima, prodaji, fakturama ili iznosima.
                - Provera kursne liste ili konverzija valuta.
                - Tehnicka pitanja (timeout, portovi, login).
                - Tehnicka pitanja vezana za EFakturu, Google OAuth, Microsoft Graph API
                
                ZABRANJENI UPITI:
                - Opsta istorija, geografija ili nauka.
                - Zabava, sport, filmovi ili recepti.
                - Pitanja koja nemaju apsolutno nikakve veze sa biznisom ili ERP sistemima.
                
                Korisnicki upit: "%s"
                """.formatted(userMessage);

        String result = this.guardrailClient.prompt()
                .user(classificationPrompt)
                .call()
                .content();

        return result != null && result.trim().toUpperCase().contains("VAN_TEME");
    }
}
