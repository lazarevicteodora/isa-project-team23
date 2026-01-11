package rs.ac.uns.ftn.isa.isa_project.dto;

import rs.ac.uns.ftn.isa.isa_project.model.Comment;
import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private Long videoId;
    private Long authorId;
    private String authorUsername;
    private String authorEmail;
    private LocalDateTime createdAt;

    public CommentDTO() {
    }

    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.videoId = comment.getVideo().getId();
        this.authorId = comment.getAuthor().getId();
        this.authorUsername = comment.getAuthor().getUsername();
        this.authorEmail = comment.getAuthor().getEmail();
        this.createdAt = comment.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
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

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}