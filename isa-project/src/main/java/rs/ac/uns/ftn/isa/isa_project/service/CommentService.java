package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rs.ac.uns.ftn.isa.isa_project.dto.CommentDTO;
import rs.ac.uns.ftn.isa.isa_project.model.Comment;
import rs.ac.uns.ftn.isa.isa_project.model.User;

public interface CommentService {

    @Cacheable(value = "video_comments", key = "#videoId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    Page<CommentDTO> getCommentsByVideoId(Long videoId, Pageable pageable);

    Comment addComment(Long videoId, String content, User user);

    @CacheEvict(value = "video_comments", allEntries = true)
    void deleteComment(Long commentId, User user);

    long getCommentCount(Long videoId);
    
    void resetRateLimits();
}