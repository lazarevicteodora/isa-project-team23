package rs.ac.uns.ftn.isa.isa_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import rs.ac.uns.ftn.isa.isa_project.model.ChatMessage;
import rs.ac.uns.ftn.isa.isa_project.service.StreamChatService;

import java.net.URI;

/**
 * WebSocket handler za streaming chat.
 * Upravlja konekcijama korisnika i prosleđuje poruke servisu.
 */
@Component
public class StreamingChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatWebSocketHandler.class);

    @Autowired
    private StreamChatService streamChatService;

    private final ObjectMapper objectMapper;

    public StreamingChatWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // ← KRITIČNO za LocalDateTime
    }

    /**
     * Izvlači videoId iz URI-ja WebSocket konekcije
     * Npr: ws://localhost:8080/ws/stream-chat/123 → videoId = 123
     */
    private Long extractVideoIdFromUri(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath(); // "/ws/stream-chat/123"
                String[] parts = path.split("/");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1];
                    return Long.parseLong(lastPart);
                }
            }
        } catch (Exception e) {
            logger.error("Greška pri ekstrakciji videoId iz URI-ja: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Poziva se kada korisnik uspostavi WebSocket konekciju.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        Long videoId = extractVideoIdFromUri(session);

        if (videoId != null) {
            logger.info("✅ Nova WebSocket konekcija za video: {} (session: {})", videoId, session.getId());
            // Dodaj sesiju u servis
            streamChatService.addUserSession(session, videoId);
        } else {
            logger.error("❌ Nije moguće ekstraktovati videoId iz URI-ja");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    /**
     * Poziva se kada korisnik pošalje poruku.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("📥 PRIMLJENI PAYLOAD: " + payload); // ← DEBUG

            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            System.out.println("🔍 PARSIRANA PORUKA NAKON DESERIALIZACIJE: " + chatMessage); // ← DEBUG

            // Ako videoId nije u poruci, dobavi ga iz sesije
            if (chatMessage.getVideoId() == null) {
                Long videoId = extractVideoIdFromUri(session);
                System.out.println("⚠️ videoId nije u poruci, ekstraktujem iz URI: " + videoId); // ← DEBUG
                chatMessage.setVideoId(videoId);
            }

            logger.info("💬 Primljena poruka: {} od {} za video: {}",
                    chatMessage.getMessage(),
                    chatMessage.getUsername(),
                    chatMessage.getVideoId());

            // Broadcast svima na tom videu
            streamChatService.broadcastMessage(chatMessage, session);

        } catch (Exception e) {
            System.err.println("❌ GREŠKA U HANDLERU: " + e.getMessage()); // ← DEBUG
            e.printStackTrace(); // ← DEBUG
            logger.error("Greška pri obradi WebSocket poruke: {}", e.getMessage(), e);
            session.sendMessage(new TextMessage("{\"error\":\"Greška pri obradi poruke\"}"));
        }
    }

    /**
     * Poziva se kada korisnik zatvori WebSocket konekciju.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        streamChatService.removeUserSession(session);
        logger.info("🔌 WebSocket konekcija zatvorena: {} (razlog: {})", session.getId(), status);
    }

    /**
     * Poziva se ako dođe do greške pri komunikaciji.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        logger.error("❌ WebSocket transport error: {}", exception.getMessage());
        streamChatService.removeUserSession(session);
    }
}