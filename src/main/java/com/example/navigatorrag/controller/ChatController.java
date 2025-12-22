package com.example.navigatorrag.controller;

import com.example.navigatorrag.dto.ChatRequest;
import com.example.navigatorrag.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public String chat(@RequestParam String role, @RequestBody ChatRequest request) {
        activeSessions.add(request.sessionId());

        String otherSessions = activeSessions.stream()
                .filter(id -> !id.equals(request.sessionId()))
                .collect(Collectors.joining(", "));

        System.out.println("--- NEW INCOMING CALL ---");
        System.out.println("Role: " + role);
        System.out.println("Session ID: " + request.sessionId());
        System.out.println("Message: " + request.message());
        System.out.println("Other Available Sessions: [" + (otherSessions.isEmpty() ? "None" : otherSessions) + "]");
        System.out.println("-------------------------");

        return chatService.generateRespone(request.sessionId(), role, request.message());
    }
}
