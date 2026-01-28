package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;

@Entity
@Table(name = "view_counts_replica_1")
public class ViewCountReplica1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, unique = true)
    private Long videoId;

    @Column(name = "count", nullable = false)
    private Long count = 0L;

    // Konstruktori
    public ViewCountReplica1() {}

    public ViewCountReplica1(Long videoId) {
        this.videoId = videoId;
        this.count = 0L;
    }

    // Getters i Setters
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
    }

    // Helper metoda za increment
    public void increment() {
        this.count++;
    }
}