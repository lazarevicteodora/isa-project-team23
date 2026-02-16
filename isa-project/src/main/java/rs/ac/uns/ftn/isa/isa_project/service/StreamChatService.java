package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.model.ChatMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Interface za streaming chat servis.
 */
public interface StreamChatService {

    /**
     * Dodaj korisnika u chat za video.
     */
    void addUserSession(WebSocketSession session, Long videoId);

    /**
     * Prosleđuje poruku svim korisnicima spojenim na taj video.
     */
    void broadcastMessage(ChatMessage chatMessage, WebSocketSession senderSession);

    /**
     * Uklanja korisnikovu sesiju iz svih chat grupa.
     */
    void removeUserSession(WebSocketSession session);

    /**
     * Dobija broj aktivnih korisnika na određenom videu.
     */
    int getActiveUsersCount(Long videoId);
}