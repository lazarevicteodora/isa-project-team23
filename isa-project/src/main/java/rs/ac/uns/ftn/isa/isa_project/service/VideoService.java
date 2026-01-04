package rs.ac.uns.ftn.isa.isa_project.service;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoResponseDTO;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoUploadDTO;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import java.util.List;

public interface VideoService {
    Video createVideo(VideoUploadDTO dto) throws Exception;
    byte[] getThumbnailContent(Long videoId) throws Exception;
    Video getVideoById(Long videoId) throws Exception;
    List<Video> getAllVideos();
    void incrementViewCount(Long videoId);
}

