package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.isa.isa_project.model.WatchPartyRoom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servis za upravljanje Watch Party sobama.
 */
@Service
public class WatchPartyService {

    private static final Logger LOG = LoggerFactory.getLogger(WatchPartyService.class);

    // Mapa: roomCode -> WatchPartyRoom
    private final Map<String, WatchPartyRoom> rooms = new ConcurrentHashMap<>();

    /**
     * Kreira novu Watch Party sobu.
     */
    public WatchPartyRoom createRoom(Long videoId, String videoTitle,
                                     Long creatorId, String creatorUsername) {
        String roomCode = generateRoomCode();
        WatchPartyRoom room = new WatchPartyRoom(roomCode, videoId, videoTitle,
                creatorId, creatorUsername);
        rooms.put(roomCode, room);

        LOG.info("Watch Party room created: {} by {} for video {}",
                roomCode, creatorUsername, videoId);

        return room;
    }

    /**
     * Pronalazi sobu po kodu.
     */
    public Optional<WatchPartyRoom> findRoom(String roomCode) {
        return Optional.ofNullable(rooms.get(roomCode));
    }

    /**
     * Dodaje člana u sobu.
     */
    public Optional<WatchPartyRoom> joinRoom(String roomCode, String username) {
        WatchPartyRoom room = rooms.get(roomCode);
        if (room == null || !room.isActive()) {
            return Optional.empty();
        }
        room.addMember(username);
        LOG.info("User {} joined Watch Party room {}", username, roomCode);
        return Optional.of(room);
    }

    /**
     * Uklanja člana iz sobe.
     */
    public void leaveRoom(String roomCode, String username) {
        WatchPartyRoom room = rooms.get(roomCode);
        if (room != null) {
            room.removeMember(username);
            LOG.info("User {} left Watch Party room {}", username, roomCode);

            // Ako nema više članova, deaktiviraj sobu
            if (room.getMemberCount() == 0) {
                room.setActive(false);
                rooms.remove(roomCode);
                LOG.info("Watch Party room {} closed (no members)", roomCode);
            }
        }
    }

    /**
     * Vraća sve aktivne sobe.
     */
    public List<WatchPartyRoom> getAllActiveRooms() {
        return new ArrayList<>(rooms.values());
    }

    /**
     * Generiše jedinstveni 6-cifreni kod sobe.
     */
    private String generateRoomCode() {
        String code;
        do {
            code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase();
        } while (rooms.containsKey(code));
        return code;
    }
}