package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "view_logs", indexes = {
        @Index(name = "idx_view_log_video_date", columnList = "video_id, view_date")
})
public class ViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    public ViewLog() {}

    public ViewLog(Video video, LocalDate viewDate) {
        this.video = video;
        this.viewDate = viewDate;
        this.viewCount = 1L;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public LocalDate getViewDate() { return viewDate; }
    public void setViewDate(LocalDate viewDate) { this.viewDate = viewDate; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public void increment() { this.viewCount++; }
}