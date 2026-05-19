package com.smartwaste.controller;

import com.smartwaste.entity.ChatMessage;
import com.smartwaste.entity.User;
import com.smartwaste.repository.ChatRepository;
import com.smartwaste.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/internal/chat")
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public ChatController(ChatRepository chatRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println(">>> CHAT DEBUG: ChatController is READY at /internal/chat");
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, String>>> getAllUsers(@AuthenticationPrincipal UserDetails userDetails) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        String myRole = me.getRole();
        
        System.out.println(">>> CHAT DEBUG: Fetching users for " + userDetails.getUsername() + " (Role: " + myRole + ")");
        try {
            // Kita gunakan query manual untuk menghindari masalah 'Primitive Null' pada entity Collector
            // dan sekaligus melakukan filter peran.
            List<Object[]> results;
            if ("CITIZEN".equals(myRole)) {
                // Citizen hanya melihat Collector dan Admin
                results = userRepository.findChatUsersForCitizen(userDetails.getUsername());
            } else {
                // Collector/Admin melihat semua kecuali dirinya sendiri
                results = userRepository.findAllChatUsersExcept(userDetails.getUsername());
            }

            List<Map<String, String>> users = results.stream()
                    .map(row -> {
                        java.util.Map<String, String> m = new java.util.HashMap<>();
                        String id = String.valueOf(row[0]);
                        m.put("id", id);
                        
                        String name = (row[1] != null && !String.valueOf(row[1]).trim().isEmpty()) ? String.valueOf(row[1]) : String.valueOf(row[2]);
                        m.put("name", name);
                        m.put("email", String.valueOf(row[2]));
                        m.put("role", String.valueOf(row[3]));
                        
                        // Fallback manual count safe approach
                        User sender = userRepository.findById(id).orElse(null);
                        long unreadCount = (sender != null) ? chatRepository.countUnreadFromSender(me, sender) : 0;
                        m.put("unreadCount", String.valueOf(unreadCount));
                        
                        return m;
                    })
                    .collect(Collectors.toList());
            
            System.out.println(">>> CHAT DEBUG: Final list size: " + users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history/{targetId}")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String targetId) {
        
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        User target = userRepository.findById(targetId).orElseThrow();

        List<ChatMessage> messages = chatRepository.findChatHistory(me, target);
        
        // Mark as read
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(me.getId()))
                .forEach(m -> {
                    m.setRead(true);
                    chatRepository.save(m);
                });

        return ResponseEntity.ok(messages.stream()
                .map(m -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId());
                    map.put("senderId", m.getSender().getId());
                    map.put("senderName", m.getSender().getName());
                    map.put("message", m.getMessage());
                    // Gunakan format ISO agar JS mudah mem-parse
                    map.put("sentAt", m.getSentAt().toString());
                    map.put("isMe", m.getSender().getId().equals(me.getId()));
                    return map;
                })
                .collect(Collectors.toList()));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {
        
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        User target = userRepository.findById(payload.get("receiverId")).orElseThrow();

        ChatMessage msg = ChatMessage.builder()
                .sender(me)
                .receiver(target)
                .message(payload.get("message"))
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        chatRepository.save(msg);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User me = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        long count = chatRepository.countUnreadMessages(me);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("unreadCount", count);
        return ResponseEntity.ok(result);
    }
}
