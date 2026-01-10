package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.model.VideoLike;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoLikeRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;

@Service
public class LikeServiceImpl implements LikeService {

    private static final Logger LOG = LoggerFactory.getLogger(LikeServiceImpl.class);

    @Autowired
    private VideoLikeRepository likeRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Override
    @Transactional
    public boolean toggleLike(Long videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));

        boolean exists = likeRepository.existsByVideoIdAndUserId(videoId, user.getId());

        if (exists) {
            // Ukloni lajk
            likeRepository.deleteByVideoIdAndUserId(videoId, user.getId());
            LOG.info("User {} unliked video {}", user.getUsername(), videoId);
            return false; // unliked
        } else {
            // Dodaj lajk
            VideoLike like = new VideoLike(video, user);
            likeRepository.save(like);
            LOG.info("User {} liked video {}", user.getUsername(), videoId);
            return true; // liked
        }
    }

    @Override
    public boolean hasUserLiked(Long videoId, Long userId) {
        return likeRepository.existsByVideoIdAndUserId(videoId, userId);
    }

    @Override
    public long getLikeCount(Long videoId) {
        return likeRepository.countByVideoId(videoId);
    }
}