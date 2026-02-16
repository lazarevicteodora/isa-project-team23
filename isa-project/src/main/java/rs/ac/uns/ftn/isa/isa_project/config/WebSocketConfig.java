package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import rs.ac.uns.ftn.isa.isa_project.controller.StreamingChatWebSocketHandler;

/**
 * Konfiguracija za WebSocket streaming chat.
 * Mapira /ws/stream-chat/{videoId} endpoint na StreamingChatWebSocketHandler.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private StreamingChatWebSocketHandler streamingChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(streamingChatWebSocketHandler, "/ws/stream-chat/**")
                .setAllowedOrigins("http://localhost:4200"); // ← SIGURNIJE!
    }
}