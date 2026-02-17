package rs.ac.uns.ftn.isa.isa_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import rs.ac.uns.ftn.isa.isa_project.model.ChatMessage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacija StreamChatService.
 * Upravlja aktivnim chat sesijama korisnika tokom streaming-a.
 */
@Service
public class StreamChatServiceImpl implements StreamChatService {

    private static final Logger logger = LoggerFactory.getLogger(StreamChatServiceImpl.class);

    /**
     * Map koji čuva sve WebSocket sesije.
     * Ključ: videoId, Vrednost: Set svih aktivnih sesija za taj video
     */
    private final Map<Long, Set<WebSocketSession>> videoSessions = new ConcurrentHashMap<>();

    /**
     * Map koji čuva mapiranje sesije na videoId.
     * Ključ: WebSocketSession, Vrednost: videoId
     */
    private final Map<WebSocketSession, Long> sessionToVideoMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public StreamChatServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void addUserSession(WebSocketSession session, Long videoId) {
        videoSessions.computeIfAbsent(videoId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
        sessionToVideoMap.put(session, videoId);

        int userCount = videoSessions.get(videoId).size();
        logger.info("👤 Korisnik dodat u chat za video: {} (ukupno: {})", videoId, userCount);

        // Pošalji svima ažurirani broj korisnika
        broadcastUserCount(videoId, userCount);
    }

    @Override
    public void broadcastMessage(ChatMessage chatMessage, WebSocketSession senderSession) {
        Long videoId = chatMessage.getVideoId();

        if (videoId == null) {
            logger.error("❌ ChatMessage nema videoId");
            return;
        }

        Set<WebSocketSession> sessions = videoSessions.get(videoId);

        if (sessions == null || sessions.isEmpty()) {
            logger.warn("⚠️ Nema aktivnih sesija za video: {}", videoId);
            return;
        }

        logger.info("📢 Broadcasting poruku za video {} - broj sesija: {}", videoId, sessions.size());

        try {
            // Serijalizuj ChatMessage u JSON
            String messageJson = objectMapper.writeValueAsString(chatMessage);
            logger.info("🔍 JSON KOJI SE ŠALJE: {}", messageJson);

            TextMessage textMessage = new TextMessage(messageJson);

            // Pošalji svim povezanim korisnicima (uključujući i pošiljaoca)
            for (WebSocketSession session : new HashSet<>(sessions)) {
                if (session.isOpen()) {
                    try {
                        logger.info("📤 Šaljem poruku na sesiju: {}", session.getId());
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("❌ Greška pri slanju poruke na sesiju: {}", e.getMessage());
                        removeUserSession(session);
                    }
                } else {
                    logger.warn("⚠️ Sesija zatvorena, uklanjam: {}", session.getId());
                    removeUserSession(session);
                }
            }

            logger.info("✅ Poruka poslata svim sesijama!");

        } catch (Exception e) {
            logger.error("❌ Greška pri broadcast-u poruke: {}", e.getMessage(), e);
        }
    }

    @Override
    public void removeUserSession(WebSocketSession session) {
        Long videoId = sessionToVideoMap.remove(session);

        if (videoId != null) {
            Set<WebSocketSession> sessions = videoSessions.get(videoId);
            if (sessions != null) {
                sessions.remove(session);
                int userCount = sessions.size();
                logger.info("👋 Korisnik napustio chat za video: {} (preostalo: {})", videoId, userCount);

                // Pošalji svima ažurirani broj korisnika
                broadcastUserCount(videoId, userCount);

                // Ako nema više korisnika, obriši celu grupu
                if (sessions.isEmpty()) {
                    videoSessions.remove(videoId);
                    logger.info("🗑️ Chat grupa za video {} obrisana (nema više korisnika)", videoId);
                }
            }
        }
    }

    @Override
    public int getActiveUsersCount(Long videoId) {
        Set<WebSocketSession> sessions = videoSessions.get(videoId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Šalje svim korisnicima u grupi ažurirani broj aktivnih korisnika.
     */
    private void broadcastUserCount(Long videoId, int count) {
        Set<WebSocketSession> sessions = videoSessions.get(videoId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            // Kreirajspecijalni USER_COUNT objekat
            Map<String, Object> userCountMessage = new HashMap<>();
            userCountMessage.put("type", "USER_COUNT");
            userCountMessage.put("videoId", videoId);
            userCountMessage.put("count", count);

            String json = objectMapper.writeValueAsString(userCountMessage);
            TextMessage textMessage = new TextMessage(json);

            logger.info("👥 Broadcast USER_COUNT za video {}: {} korisnika", videoId, count);

            for (WebSocketSession session : new HashSet<>(sessions)) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("Greška pri slanju USER_COUNT: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Greška pri broadcast-u USER_COUNT: {}", e.getMessage(), e);
        }
    }
}