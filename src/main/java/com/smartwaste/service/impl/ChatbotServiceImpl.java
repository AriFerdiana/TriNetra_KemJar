package com.smartwaste.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartwaste.dto.request.ChatRequest;
import com.smartwaste.dto.response.ChatResponse;
import com.smartwaste.entity.ChatLog;
import com.smartwaste.entity.Citizen;
import com.smartwaste.repository.ChatLogRepository;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.service.ChatbotService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementasi AI Chatbot menggunakan Mistral AI REST API via OkHttp.
 *
 * <p>Melakukan HTTP POST ke Mistral API endpoint:
 * {@code https://api.mistral.ai/v1/chat/completions}</p>
 *
 * <p>Setiap percakapan disimpan ke database {@link ChatLog} untuk audit trail.</p>
 */
@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatbotServiceImpl.class);

    private final ChatLogRepository chatLogRepository;
    private final CitizenRepository citizenRepository;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChatbotServiceImpl(ChatLogRepository chatLogRepository,
                              CitizenRepository citizenRepository,
                              OkHttpClient httpClient,
                              ObjectMapper objectMapper) {
        this.chatLogRepository = chatLogRepository;
        this.citizenRepository = citizenRepository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Value("${app.mistral.api-key}")
    private String mistralApiKey;

    @Value("${app.mistral.base-url}")
    private String mistralBaseUrl;

    @Value("${app.mistral.model}")
    private String mistralModel;

    @Value("${app.mistral.max-tokens}")
    private int maxTokens;

    @Value("${app.mistral.temperature}")
    private double temperature;

    @Value("${app.mistral.system-prompt}")
    private String systemPrompt;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request, String citizenEmail) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail).orElse(null);
        String sessionId = request.getSessionId() != null ?
                request.getSessionId() : UUID.randomUUID().toString();

        String botResponse = callMistralApi(request.getMessage());

        // Simpan log percakapan
        ChatLog log = new ChatLog(citizen, request.getMessage(), botResponse, sessionId, mistralModel);
        chatLogRepository.save(log);

        return ChatResponse.builder()
                .message(botResponse)
                .sessionId(sessionId)
                .aiModel(mistralModel)
                .success(botResponse != null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ChatResponse chatAnonymous(ChatRequest request) {
        String sessionId = request.getSessionId() != null ?
                request.getSessionId() : UUID.randomUUID().toString();
        String anonId = request.getAnonymousIdentifier() != null ?
                request.getAnonymousIdentifier() : UUID.randomUUID().toString();

        String botResponse = callMistralApi(request.getMessage());

        ChatLog chatLog = new ChatLog(anonId, request.getMessage(), botResponse, sessionId);
        chatLogRepository.save(chatLog);

        return ChatResponse.builder()
                .message(botResponse)
                .sessionId(sessionId)
                .aiModel(mistralModel)
                .success(botResponse != null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Memanggil Mistral AI REST API menggunakan OkHttp.
     *
     * @param userMessage pesan dari user
     * @return respons teks dari AI, atau pesan error fallback
     */
    private String callMistralApi(String userMessage) {
        try {
            // Bangun request body JSON
            String requestBodyJson = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                put("model", mistralModel);
                put("max_tokens", maxTokens);
                put("temperature", temperature);
                put("messages", new java.util.ArrayList<>() {{
                    add(new java.util.HashMap<>() {{
                        put("role", "system");
                        put("content", systemPrompt);
                    }});
                    add(new java.util.HashMap<>() {{
                        put("role", "user");
                        put("content", userMessage);
                    }});
                }});
            }});

            Request httpRequest = new Request.Builder()
                    .url(mistralBaseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + mistralApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBodyJson, JSON))
                    .build();

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful() || httpResponse.body() == null) {
                    log.error("Mistral API error: HTTP {}", httpResponse.code());
                    return getFallbackResponse(userMessage);
                }

                String responseBody = httpResponse.body().string();
                JsonNode json = objectMapper.readTree(responseBody);
                return json.path("choices").get(0)
                           .path("message").path("content").asText();
            }

        } catch (Exception e) {
            log.error("Error saat memanggil Mistral API: {}", e.getMessage());
            return getFallbackResponse(userMessage);
        }
    }

    /** Respons fallback jika API tidak tersedia */
    private String getFallbackResponse(String userMessage) {
        if (userMessage.toLowerCase().contains("poin") || userMessage.toLowerCase().contains("berapa")) {
            return "Mohon maaf, layanan AI sedang tidak tersedia. " +
                   "Estimasi poin: Organik 5 poin/kg, Anorganik 15 poin/kg, B3 60 poin/kg (sudah termasuk bonus). " +
                   "Silakan coba lagi nanti.";
        }
        return "Mohon maaf, layanan AI chatbot sedang dalam pemeliharaan. " +
               "Silakan hubungi petugas atau coba beberapa saat lagi.";
    }
}
