package rs.ac.uns.ftn.isa.isa_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {

    @NotBlank(message = "Content cannot be empty")
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String content;

    public CommentRequest() {
    }

    public CommentRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}