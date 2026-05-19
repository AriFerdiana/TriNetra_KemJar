package com.smartwaste.controller;

import com.smartwaste.dto.request.ChatRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.ChatResponse;
import com.smartwaste.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chatbot AI", description = "Integrasi Mistral AI untuk panduan daur ulang")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /** Chat dengan login (citizen) */
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Chat dengan Mistral AI (Citizen)", description = "Tanya panduan daur ulang, estimasi poin, dll.")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request, Authentication auth) {
        ChatResponse response = (auth != null && auth.isAuthenticated())
                ? chatbotService.chat(request, auth.getName())
                : chatbotService.chatAnonymous(request);
        return ResponseEntity.ok(ApiResponse.success("Respons AI diterima.", response));
    }

    /** Chat anonim (dari landing page, tanpa login) */
    @PostMapping("/anonymous")
    @Operation(summary = "Chat anonim dari landing page")
    public ResponseEntity<ApiResponse<ChatResponse>> chatAnonymous(
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Respons AI diterima.",
                chatbotService.chatAnonymous(request)));
    }
}
