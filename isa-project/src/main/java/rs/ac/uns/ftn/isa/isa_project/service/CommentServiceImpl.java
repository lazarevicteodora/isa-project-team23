package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.dto.CommentDTO;
import rs.ac.uns.ftn.isa.isa_project.model.Comment;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.CommentRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger LOG = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    // Per-user locks za thread-safe rate limiting
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    @Override
    public Page<CommentDTO> getCommentsByVideoId(Long videoId, Pageable pageable) {
        LOG.info("Fetching comments for video {} (page {}, size {})",
                videoId, pageable.getPageNumber(), pageable.getPageSize());
        Page<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);
        return comments.map(CommentDTO::new);
    }

    @Override
    @Transactional
    @CacheEvict(value = "video_comments", allEntries = true)
    public Comment addComment(Long videoId, String content, User user) {
        // Dobij lock za ovog korisnika (thread-safe)
        ReentrantLock lock = userLocks.computeIfAbsent(user.getId(), id -> new ReentrantLock());

        lock.lock(); // Zaključaj - drugi thread-ovi čekaju ovde
        try {
            // KRITIČNA SEKCIJA - samo jedan thread odjednom za istog korisnika
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long commentCount = commentRepository.countByUserIdSince(user.getId(), oneHourAgo);

            if (commentCount >= 60) {
                LOG.warn("User {} exceeded comment rate limit (60/hour)", user.getUsername());
                throw new RuntimeException("You have exceeded the maximum number of comments per hour (60). Please try again later.");
            }

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));

            Comment comment = new Comment(content, video, user);
            comment = commentRepository.save(comment);

            LOG.info("User {} added comment to video {}", user.getUsername(), videoId);
            return comment;

        } finally {
            lock.unlock(); // UVEK otključaj, čak i ako dođe do exception-a
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        commentRepository.delete(comment);
        LOG.info("Comment {} deleted by user {}", commentId, user.getUsername());
    }

    @Override
    public long getCommentCount(Long videoId) {
        return commentRepository.countByVideoId(videoId);
    }

    @Override
    @Transactional
    public void resetRateLimits() {
        userLocks.clear();
        LOG.info("Rate limits reset (based on DB timestamps)");
    }
}