package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;

/**
 * Entity klasa koja zamenjuje ViewCountReplica1 i ViewCountReplica2.
 */
@Entity
@Table(name = "view_counts")
@IdClass(ViewCountId.class)
public class ViewCount {

    @Id
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Id
    @Column(name = "replica_id", nullable = false, length = 50)
    private String replicaId;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    // Konstruktori
    public ViewCount() {
    }

    public ViewCount(Long videoId, String replicaId) {
        this.videoId = videoId;
        this.replicaId = replicaId;
        this.count = 0L;
    }

    public ViewCount(Long videoId, String replicaId, Long count) {
        this.videoId = videoId;
        this.replicaId = replicaId;
        this.count = count;
    }

    // Getters i Setters
    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(String replicaId) {
        this.replicaId = replicaId;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    // Helper metoda za increment
    public void increment() {
        this.count++;
    }

    public void increment(long amount) {
        this.count += amount;
    }

    @Override
    public String toString() {
        return "ViewCount{" +
                "videoId=" + videoId +
                ", replicaId='" + replicaId + '\'' +
                ", count=" + count +
                '}';
    }
}