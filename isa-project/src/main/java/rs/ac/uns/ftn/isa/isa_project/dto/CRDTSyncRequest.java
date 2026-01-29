package rs.ac.uns.ftn.isa.isa_project.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO za sinhronizaciju CRDT stanja između replika.
 *
 * Koristi se za:
 * - Push-based sync (jedna replika šalje svoj update drugima)
 * - Pull-based sync (jedna replika traži stanje od drugih)
 */
public class CRDTSyncRequest {

    private Long videoId;
    private String sourceReplicaId;

    // Map: replicaId -> count za tu repliku
    private Map<String, Long> counts;

    // Konstruktori
    public CRDTSyncRequest() {
        this.counts = new HashMap<>();
    }

    public CRDTSyncRequest(Long videoId, String sourceReplicaId, Map<String, Long> counts) {
        this.videoId = videoId;
        this.sourceReplicaId = sourceReplicaId;
        this.counts = counts != null ? new HashMap<>(counts) : new HashMap<>();
    }

    // Getters and Setters
    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getSourceReplicaId() {
        return sourceReplicaId;
    }

    public void setSourceReplicaId(String sourceReplicaId) {
        this.sourceReplicaId = sourceReplicaId;
    }

    public Map<String, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }

    // Helper metode
    public void addCount(String replicaId, Long count) {
        this.counts.put(replicaId, count);
    }

    public Long getCountForReplica(String replicaId) {
        return counts.getOrDefault(replicaId, 0L);
    }

    @Override
    public String toString() {
        return "CRDTSyncRequest{" +
                "videoId=" + videoId +
                ", sourceReplicaId='" + sourceReplicaId + '\'' +
                ", counts=" + counts +
                '}';
    }
}