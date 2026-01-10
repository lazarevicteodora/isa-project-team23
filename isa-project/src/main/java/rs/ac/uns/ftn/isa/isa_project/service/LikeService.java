package rs.ac.uns.ftn.isa.isa_project.service;

import rs.ac.uns.ftn.isa.isa_project.model.User;

public interface LikeService {
    boolean toggleLike(Long videoId, User user);
    boolean hasUserLiked(Long videoId, Long userId);
    long getLikeCount(Long videoId);
}