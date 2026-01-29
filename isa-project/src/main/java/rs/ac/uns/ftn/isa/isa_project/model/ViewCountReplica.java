package rs.ac.uns.ftn.isa.isa_project.model;

/**
 * DTO klasa za view count repliku.
 *
 * VAŽNO: Ovo NIJE JPA @Entity!
 * Svaka replika ima svoju odvojenu tabelu (view_counts_replica_1, view_counts_replica_2, itd.)
 * ViewCountReplicaDAO koristi Native SQL za pristup ovim tabelama.
 */
public class ViewCountReplica {

    private Long id;
    private Long videoId;
    private Long count;
    private Long lastUpdated;
    private String replicaId;

    // Konstruktori
    public ViewCountReplica() {
        this.count = 0L;
        this.lastUpdated = System.currentTimeMillis();
    }

    public ViewCountReplica(Long videoId) {
        this.videoId = videoId;
        this.count = 0L;
        this.lastUpdated = System.currentTimeMillis();
    }

    public ViewCountReplica(Long videoId, String replicaId) {
        this.videoId = videoId;
        this.replicaId = replicaId;
        this.count = 0L;
        this.lastUpdated = System.currentTimeMillis();
    }

    public ViewCountReplica(Long videoId, Long count) {
        this.videoId = videoId;
        this.count = count;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(String replicaId) {
        this.replicaId = replicaId;
    }

    // Helper metode
    public void increment() {
        if (this.count == null) {
            this.count = 0L;
        }
        this.count++;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void increment(long amount) {
        if (this.count == null) {
            this.count = 0L;
        }
        this.count += amount;
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "ViewCountReplica{" +
                "id=" + id +
                ", videoId=" + videoId +
                ", count=" + count +
                ", replicaId='" + replicaId + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}