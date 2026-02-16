package rs.ac.uns.ftn.isa.isa_project.dto;

import rs.ac.uns.ftn.isa.isa_project.model.Video;
import java.time.LocalDateTime;
import java.util.Set;

public class VideoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Set<String> tags;
    private String thumbnailUrl;
    private String videoUrl;
    private LocalDateTime createdAt;
    private String authorUsername;
    private Long authorId;
    private Long viewCount;
    private Double latitude;
    private Double longitude;

    private Long likeCount = 0L;
    private Long commentCount = 0L;
    private Boolean isScheduled;
    private LocalDateTime scheduledFor;
    private String streamingStatus; // "UPCOMING", "LIVE", "ENDED", ili null
    private Long currentOffset; // u sekundama - koliko je video već prošao    public VideoResponseDTO() {}

    public VideoResponseDTO(Video video) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.tags = video.getTags();
        this.thumbnailUrl = "/api/videos/" + video.getId() + "/thumbnail";
        this.videoUrl = "/api/videos/" + video.getId() + "/stream";
        this.createdAt = video.getCreatedAt();
        this.authorUsername = video.getAuthor().getUsername();
        this.authorId = video.getAuthor().getId();
        this.viewCount = video.getViewCount();
        this.latitude = video.getLatitude();
        this.longitude = video.getLongitude();
        this.isScheduled = video.getIsScheduled();
        this.scheduledFor = video.getScheduledFor();


// Izračunaj streaming status i offset
        if (video.getIsScheduled() != null && video.getIsScheduled()) {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(video.getScheduledFor())) {
                this.streamingStatus = "UPCOMING";
                this.currentOffset = null;
            } else {
                this.streamingStatus = "LIVE";
                // Koliko sekundi je prošlo od zakazanog vremena
                long seconds = java.time.Duration.between(video.getScheduledFor(), now).getSeconds();
                this.currentOffset = Math.max(0, seconds);
            }
        } else {
            this.streamingStatus = null; // normalan video
            this.currentOffset = null;
        }

    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }
    public Boolean getIsScheduled() {
        return isScheduled;
    }

    public void setIsScheduled(Boolean scheduled) {
        isScheduled = scheduled;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public String getStreamingStatus() {
        return streamingStatus;
    }

    public void setStreamingStatus(String streamingStatus) {
        this.streamingStatus = streamingStatus;
    }

    public Long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(Long currentOffset) {
        this.currentOffset = currentOffset;
    }
}