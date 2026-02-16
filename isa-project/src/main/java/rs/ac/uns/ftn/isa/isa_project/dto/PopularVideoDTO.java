package rs.ac.uns.ftn.isa.isa_project.dto;

import rs.ac.uns.ftn.isa.isa_project.model.PopularVideoResult;
import java.time.LocalDateTime;

public class PopularVideoDTO {

    private Long videoId;
    private String title;
    private String description;
    private String authorUsername;
    private String thumbnailUrl;
    private String videoUrl;
    private Double popularityScore;
    private Integer rankPosition;
    private LocalDateTime pipelineRunAt;

    public PopularVideoDTO() {}

    public PopularVideoDTO(PopularVideoResult result) {
        this.videoId = result.getVideo().getId();
        this.title = result.getVideo().getTitle();
        this.description = result.getVideo().getDescription();
        this.authorUsername = result.getVideo().getAuthor().getUsername();
        this.thumbnailUrl = "/api/videos/" + result.getVideo().getId() + "/thumbnail";
        this.videoUrl = "/api/videos/" + result.getVideo().getId() + "/stream";
        this.popularityScore = result.getPopularityScore();
        this.rankPosition = result.getRankPosition();
        this.pipelineRunAt = result.getRunAt();
    }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public LocalDateTime getPipelineRunAt() { return pipelineRunAt; }
    public void setPipelineRunAt(LocalDateTime pipelineRunAt) { this.pipelineRunAt = pipelineRunAt; }
}