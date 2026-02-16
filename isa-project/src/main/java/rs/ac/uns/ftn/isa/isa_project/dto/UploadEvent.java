package rs.ac.uns.ftn.isa.isa_project.dto;

import java.time.LocalDateTime;

public class UploadEvent {

    private Long videoId;
    private String title;
    private Long fileSize;
    private String authorUsername;
    private String videoPath;
    private LocalDateTime uploadedAt;

    public UploadEvent() {}

    public UploadEvent(Long videoId, String title, Long fileSize,
                       String authorUsername, String videoPath, LocalDateTime uploadedAt) {
        this.videoId = videoId;
        this.title = title;
        this.fileSize = fileSize;
        this.authorUsername = authorUsername;
        this.videoPath = videoPath;
        this.uploadedAt = uploadedAt;
    }

    // Getters and setters
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}