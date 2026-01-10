package rs.ac.uns.ftn.isa.isa_project.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.dto.CommentDTO;
import rs.ac.uns.ftn.isa.isa_project.dto.CommentRequest;
import rs.ac.uns.ftn.isa.isa_project.model.Comment;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.service.CommentService;

@RestController
@RequestMapping("/api/videos/{videoId}/comments")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentDTO> comments = commentService.getCommentsByVideoId(videoId, pageable);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addComment(
            @PathVariable Long videoId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {

        try {
            Comment comment = commentService.addComment(videoId, request.getContent(), user);
            CommentDTO commentDTO = new CommentDTO(comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(commentDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long videoId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User user) {

        try {
            commentService.deleteComment(commentId, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long videoId) {
        long count = commentService.getCommentCount(videoId);
        return ResponseEntity.ok(count);
    }

    static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}