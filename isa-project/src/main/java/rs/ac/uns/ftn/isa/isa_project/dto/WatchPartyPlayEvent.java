package rs.ac.uns.ftn.isa.isa_project.dto;

/**
 * DTO za WebSocket play event poruku.
 *
 * Klijent šalje ovu poruku na /app/watch-party/play
 */
public class WatchPartyPlayEvent {
    private String roomCode;
    private String action; // "play"

    public WatchPartyPlayEvent() {
    }

    public WatchPartyPlayEvent(String roomCode, String action) {
        this.roomCode = roomCode;
        this.action = action;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}