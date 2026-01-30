package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;

/**
 * Generička entity klasa za view count.
 *
 * KLJUČNA IZMENA: Ne koristi @Table anotaciju!
 * Ime tabele se dinamički postavlja u runtime-u.
 */
@Entity
public class ViewCount {

    @Id
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    // Transient field - ne perzistuje se
    @Transient
    private String replicaId;

    public ViewCount() {
    }

    public ViewCount(Long videoId) {
        this.videoId = videoId;
        this.count = 0L;
    }

    public ViewCount(Long videoId, String replicaId) {
        this.videoId = videoId;
        this.replicaId = replicaId;
        this.count = 0L;
    }

    // Getters and Setters
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
    }

    public String getReplicaId() {
        return replicaId;
    }

    public void setReplicaId(String replicaId) {
        this.replicaId = replicaId;
    }

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