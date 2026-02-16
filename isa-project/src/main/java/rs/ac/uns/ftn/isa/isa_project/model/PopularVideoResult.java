package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "popular_video_results")
public class PopularVideoResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vreme kada je pipeline pokrenut
    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    // Rang (1, 2 ili 3)
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "popularity_score", nullable = false)
    private Double popularityScore;

    public PopularVideoResult() {}

    public PopularVideoResult(LocalDateTime runAt, Integer rankPosition, Video video, Double popularityScore) {
        this.runAt = runAt;
        this.rankPosition = rankPosition;
        this.video = video;
        this.popularityScore = popularityScore;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }
}