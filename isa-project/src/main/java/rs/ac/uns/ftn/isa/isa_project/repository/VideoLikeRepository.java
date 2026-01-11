package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.VideoLike;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {

    boolean existsByVideoIdAndUserId(Long videoId, Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM VideoLike vl WHERE vl.video.id = :videoId AND vl.user.id = :userId")
    void deleteByVideoIdAndUserId(@Param("videoId") Long videoId, @Param("userId") Long userId);

    long countByVideoId(Long videoId);

    VideoLike findByVideoIdAndUserId(Long videoId, Long userId);
}