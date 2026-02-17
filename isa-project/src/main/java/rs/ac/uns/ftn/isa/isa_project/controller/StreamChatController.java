package rs.ac.uns.ftn.isa.isa_project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.service.StreamChatService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller za Stream Chat
 */
@RestController
@RequestMapping("/api/stream-chat")
@CrossOrigin(origins = "http://localhost:4200")
public class StreamChatController {

    @Autowired
    private StreamChatService streamChatService;

    /**
     * Endpoint za dobijanje broja aktivnih korisnika na videu
     */
    @GetMapping("/active-users/{videoId}")
    public ResponseEntity<Map<String, Integer>> getActiveUsers(@PathVariable Long videoId) {
        int activeUsers = streamChatService.getActiveUsersCount(videoId);

        Map<String, Integer> response = new HashMap<>();
        response.put("activeUsers", activeUsers);

        return ResponseEntity.ok(response);
    }
}