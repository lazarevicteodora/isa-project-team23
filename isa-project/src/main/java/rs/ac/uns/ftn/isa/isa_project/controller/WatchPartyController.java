package rs.ac.uns.ftn.isa.isa_project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.dto.WatchPartyCreateRequest;
import rs.ac.uns.ftn.isa.isa_project.dto.WatchPartyPlayEvent;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.WatchPartyRoom;
import rs.ac.uns.ftn.isa.isa_project.service.WatchPartyService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/watch-party")
@CrossOrigin(origins = "http://localhost:4200")
public class WatchPartyController {

    private static final Logger LOG = LoggerFactory.getLogger(WatchPartyController.class);

    @Autowired
    private WatchPartyService watchPartyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("username", user.getUsername()));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestBody WatchPartyCreateRequest request,
            @AuthenticationPrincipal User user) {

        LOG.info("📺 Creating Watch Party room for video {} by user {}",
                request.getVideoId(), user.getUsername());

        try {
            WatchPartyRoom room = watchPartyService.createRoom(
                    request.getVideoId(),
                    request.getVideoTitle(),
                    user.getId(),
                    user.getUsername()
            );

            LOG.info("✅ Room created: {}", room.getRoomCode());
            return ResponseEntity.ok(buildRoomResponse(room));
        } catch (Exception e) {
            LOG.error("❌ Error creating room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/join/{roomCode}")
    public ResponseEntity<?> joinRoom(
            @PathVariable String roomCode,
            @AuthenticationPrincipal User user) {

        LOG.info("🚪 User {} joining room {}", user.getUsername(), roomCode);

        try {
            Optional<WatchPartyRoom> roomOpt = watchPartyService.joinRoom(roomCode, user.getUsername());

            if (roomOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Room not found or inactive"));
            }

            WatchPartyRoom room = roomOpt.get();

            Map<String, Object> event = new HashMap<>();
            event.put("type", "USER_JOINED");
            event.put("username", user.getUsername());
            event.put("memberCount", room.getMemberCount());
            event.put("members", new ArrayList<>(room.getMembers()));

            messagingTemplate.convertAndSend("/topic/room/" + roomCode, (Object) event);

            LOG.info("✅ User {} joined room {}", user.getUsername(), roomCode);
            return ResponseEntity.ok(buildRoomResponse(room));
        } catch (Exception e) {
            LOG.error("❌ Error joining room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/leave/{roomCode}")
    public ResponseEntity<?> leaveRoom(
            @PathVariable String roomCode,
            @AuthenticationPrincipal User user) {

        LOG.info("👋 User {} leaving room {}", user.getUsername(), roomCode);

        try {
            watchPartyService.leaveRoom(roomCode, user.getUsername());

            Map<String, Object> event = new HashMap<>();
            event.put("type", "USER_LEFT");
            event.put("username", user.getUsername());

            messagingTemplate.convertAndSend("/topic/room/" + roomCode, (Object) event);

            return ResponseEntity.ok(Map.of("message", "Left room successfully"));
        } catch (Exception e) {
            LOG.error("❌ Error leaving room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/room/{roomCode}")
    public ResponseEntity<?> getRoom(@PathVariable String roomCode) {
        Optional<WatchPartyRoom> roomOpt = watchPartyService.findRoom(roomCode);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Room not found"));
        }
        return ResponseEntity.ok(buildRoomResponse(roomOpt.get()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getAllRooms() {
        List<Map<String, Object>> rooms = new ArrayList<>();
        watchPartyService.getAllActiveRooms().forEach(room -> rooms.add(buildRoomResponse(room)));
        return ResponseEntity.ok(rooms);
    }

    @MessageMapping("/watch-party/play")
    public void handlePlayEvent(@Payload WatchPartyPlayEvent playEvent, Principal principal) {
        String roomCode = playEvent.getRoomCode();
        LOG.info("▶️ Play event for room {} from {}", roomCode, principal.getName());

        Optional<WatchPartyRoom> roomOpt = watchPartyService.findRoom(roomCode);
        if (roomOpt.isEmpty()) return;

        WatchPartyRoom room = roomOpt.get();
        String username = getUsernameFromPrincipal(principal);

        if (!room.getCreatorUsername().equals(username)) {
            LOG.warn("⚠️ User {} is not creator of room {}", username, roomCode);
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("type", "VIDEO_STARTED");
        event.put("videoId", room.getVideoId());
        event.put("videoTitle", room.getVideoTitle());
        event.put("startedBy", username);

        messagingTemplate.convertAndSend("/topic/room/" + roomCode, (Object) event);
        LOG.info("✅ VIDEO_STARTED broadcast to room {} — videoId={}", roomCode, room.getVideoId());
    }

    private Map<String, Object> buildRoomResponse(WatchPartyRoom room) {
        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", room.getRoomCode());
        response.put("videoId", room.getVideoId());
        response.put("videoTitle", room.getVideoTitle());
        response.put("creatorId", room.getCreatorId());
        response.put("creatorUsername", room.getCreatorUsername());
        response.put("members", new ArrayList<>(room.getMembers()));
        response.put("memberCount", room.getMemberCount());
        response.put("active", room.isActive());
        response.put("createdAt", room.getCreatedAt());
        return response;
    }

    private String getUsernameFromPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            Object obj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (obj instanceof UserDetails) {
                return ((UserDetails) obj).getUsername();
            }
        }
        return principal.getName();
    }
}