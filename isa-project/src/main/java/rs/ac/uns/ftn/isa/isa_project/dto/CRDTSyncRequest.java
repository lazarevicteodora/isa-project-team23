package rs.ac.uns.ftn.isa.isa_project.dto;

import java.util.Map;

/**
 * DTO za razmenu CRDT stanja između replika.
 * Sadrži G-Counter mapu (replicaId -> count) za određeni video.
 */
public class CRDTSyncRequest {

    private Long videoId;
    private String sourceReplicaId;
    private Map<String, Long> counts; // G-Counter mapa: replicaId -> count

    public CRDTSyncRequest() {
    }

    public CRDTSyncRequest(Long videoId, String sourceReplicaId, Map<String, Long> counts) {
        this.videoId = videoId;
        this.sourceReplicaId = sourceReplicaId;
        this.counts = counts;
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

    @Override
    public String toString() {
        return "CRDTSyncRequest{" +
                "videoId=" + videoId +
                ", sourceReplicaId='" + sourceReplicaId + '\'' +
                ", counts=" + counts +
                '}';
    }
}