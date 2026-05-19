package com.smartwaste.service;

import com.smartwaste.dto.request.ChatRequest;
import com.smartwaste.dto.response.ChatResponse;

/**
 * Service interface untuk integrasi AI Chatbot Mistral.
 */
public interface ChatbotService {
    ChatResponse chat(ChatRequest request, String citizenEmail);
    ChatResponse chatAnonymous(ChatRequest request);
}
