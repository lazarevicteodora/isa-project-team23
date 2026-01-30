package rs.ac.uns.ftn.isa.isa_project.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Kompozitni primarni ključ za ViewCount tabelu.
 * Omogućava jedinstvenu identifikaciju svakog zapisa pomoću (videoId, replicaId).
 */
public class ViewCountId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long videoId;
    private String replicaId;

    // Konstruktori
    public ViewCountId() {
    }

    public ViewCountId(Long videoId, String replicaId) {
        this.videoId = videoId;
        this.replicaId = replicaId;
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

    // equals i hashCode su OBAVEZNI za @IdClass
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewCountId that = (ViewCountId) o;
        return Objects.equals(videoId, that.videoId) &&
                Objects.equals(replicaId, that.replicaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoId, replicaId);
    }

    @Override
    public String toString() {
        return "ViewCountId{" +
                "videoId=" + videoId +
                ", replicaId='" + replicaId + '\'' +
                '}';
    }
}