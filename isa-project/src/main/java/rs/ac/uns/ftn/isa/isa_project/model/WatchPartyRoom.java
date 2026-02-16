package rs.ac.uns.ftn.isa.isa_project.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Model Watch Party sobe.
 * Čuva se u memoriji (ne u bazi) jer je privremena.
 */
public class WatchPartyRoom {

    private String roomCode;        // Jedinstveni kod sobe (npr. "ABC123")
    private Long videoId;           // ID videa koji se gleda
    private String videoTitle;      // Naziv videa
    private Long creatorId;         // ID kreatora sobe
    private String creatorUsername; // Username kreatora
    private Set<String> members;    // Usernames članova sobe
    private LocalDateTime createdAt;
    private boolean active;

    public WatchPartyRoom() {
        this.members = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public WatchPartyRoom(String roomCode, Long videoId, String videoTitle,
                          Long creatorId, String creatorUsername) {
        this();
        this.roomCode = roomCode;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.creatorId = creatorId;
        this.creatorUsername = creatorUsername;
        this.members.add(creatorUsername);
    }

    // Getters and Setters
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public Set<String> getMembers() { return members; }
    public void setMembers(Set<String> members) { this.members = members; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public void addMember(String username) { this.members.add(username); }
    public void removeMember(String username) { this.members.remove(username); }
    public int getMemberCount() { return this.members.size(); }
}