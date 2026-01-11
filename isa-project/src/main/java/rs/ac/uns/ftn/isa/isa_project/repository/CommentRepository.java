package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.isa.isa_project.model.Comment;

import java.time.LocalDateTime;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId, Pageable pageable);

    long countByVideoId(Long videoId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.author.id = :userId AND c.createdAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}