package rs.ac.uns.ftn.isa.isa_project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.controller.StreamingChatWebSocketHandler;
import rs.ac.uns.ftn.isa.isa_project.util.TokenUtils;
import rs.ac.uns.ftn.isa.isa_project.service.CustomUserDetailsService;

/**
 * Jedinstvena WebSocket konfiguracija za:
 * 1. Watch Party (STOMP WebSocket na /ws)
 * 2. Stream Chat (Raw WebSocket na /ws/stream-chat/{videoId})
 */
@Configuration
@EnableWebSocketMessageBroker  // Za STOMP (Watch Party)
@EnableWebSocket               // Za Raw WebSocket (Stream Chat)
public class WatchPartyConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WatchPartyConfig.class);

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private StreamingChatWebSocketHandler streamingChatWebSocketHandler;

    // ========== STOMP WebSocket (Watch Party) ==========

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        LOG.info("✅ STOMP WebSocket endpoint registered: /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");

        LOG.info("✅ Message broker configured");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            String email = tokenUtils.getEmailFromToken(token);

                            if (email != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                                if (tokenUtils.validateToken(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails, null, userDetails.getAuthorities()
                                            );

                                    accessor.setUser(authentication);
                                    SecurityContextHolder.getContext().setAuthentication(authentication);

                                    LOG.info("✅ WebSocket authenticated: {}", email);
                                }
                            }
                        } catch (Exception e) {
                            LOG.error("❌ JWT validation failed: {}", e.getMessage());
                        }
                    }
                }

                return message;
            }
        });
    }

    // ========== Raw WebSocket (Stream Chat) ==========

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        LOG.info("🔧 Registering RAW WebSocket handler for Stream Chat...");

        registry.addHandler(streamingChatWebSocketHandler, "/ws/stream-chat/{videoId}")
                .setAllowedOriginPatterns("*");  // Dovoljno je samo ovo!

        LOG.info("✅ RAW WebSocket endpoint registered: /ws/stream-chat/{videoId}");
    }
}