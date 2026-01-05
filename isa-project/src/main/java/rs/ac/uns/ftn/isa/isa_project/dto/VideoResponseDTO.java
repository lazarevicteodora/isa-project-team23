package rs.ac.uns.ftn.isa.isa_project.dto;

import rs.ac.uns.ftn.isa.isa_project.model.Video;
import java.time.LocalDateTime;
import java.util.Set;

public class VideoResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorUsername;
    private Double latitude;
    private Double longitude;
    private String videoUrl;
    private String thumbnailUrl;
    private Long viewCount;  //

    public VideoResponseDTO() {
        super();
    }
    public VideoResponseDTO(Video video) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.tags = video.getTags();
        this.createdAt = video.getCreatedAt();
        this.latitude = video.getLatitude();
        this.longitude = video.getLongitude();
        this.viewCount = video.getViewCount();
        //  ID i username autora
        if (video.getAuthor() != null) {
            this.authorId = video.getAuthor().getId();
            this.authorUsername = video.getAuthor().getUsername();
        }
        this.videoUrl = "http://localhost:8080/api/videos/" + video.getId() + "/stream";
        this.thumbnailUrl = "http://localhost:8080/api/videos/" + video.getId() + "/thumbnail";


    }


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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

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
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Long getViewCount() {
        return viewCount;
    }
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}