package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity za praćenje transcoding poslova.
 * Čuva informacije o statusu, output fajlovima i greškama.
 */
@Entity
@Table(name = "transcoding_jobs")
public class TranscodingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", unique = true, nullable = false)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TranscodingStatus status;

    @Column(name = "original_path", nullable = false)
    private String originalPath;

    @ElementCollection
    @CollectionTable(
            name = "transcoding_output_files",
            joinColumns = @JoinColumn(name = "transcoding_job_id")
    )
    @Column(name = "output_path")
    private List<String> outputPaths = new ArrayList<>();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "progress")
    private Integer progress = 0; // 0-100

    @Column(name = "consumer_id")
    private String consumerId; // Koji consumer obrađuje job

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructors
    public TranscodingJob() {
    }

    public TranscodingJob(String jobId, Video video, String originalPath) {
        this.jobId = jobId;
        this.video = video;
        this.originalPath = originalPath;
        this.status = TranscodingStatus.PENDING;
    }

    // Helper methods
    public void markAsProcessing(String consumerId) {
        this.status = TranscodingStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        this.consumerId = consumerId;
    }

    public void markAsCompleted() {
        this.status = TranscodingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progress = 100;
    }

    public void markAsFailed(String errorMessage) {
        this.status = TranscodingStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void addOutputPath(String path) {
        this.outputPaths.add(path);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public TranscodingStatus getStatus() {
        return status;
    }

    public void setStatus(TranscodingStatus status) {
        this.status = status;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public List<String> getOutputPaths() {
        return outputPaths;
    }

    public void setOutputPaths(List<String> outputPaths) {
        this.outputPaths = outputPaths;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "TranscodingJob{" +
                "id=" + id +
                ", jobId='" + jobId + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                ", consumerId='" + consumerId + '\'' +
                '}';
    }
}