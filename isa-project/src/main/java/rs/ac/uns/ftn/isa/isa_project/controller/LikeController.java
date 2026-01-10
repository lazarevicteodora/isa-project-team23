package rs.ac.uns.ftn.isa.isa_project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.service.LikeService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos/{videoId}/likes")
@CrossOrigin(origins = "http://localhost:4200")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User user) {

        boolean liked = likeService.toggleLike(videoId, user);
        long likeCount = likeService.getLikeCount(videoId);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("likeCount", likeCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long videoId) {
        long count = likeService.getLikeCount(videoId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getLikeStatus(
            @PathVariable Long videoId,
            @AuthenticationPrincipal User user) {

        boolean liked = false;
        if (user != null) {
            liked = likeService.hasUserLiked(videoId, user.getId());
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("liked", liked);

        return ResponseEntity.ok(response);
    }
}