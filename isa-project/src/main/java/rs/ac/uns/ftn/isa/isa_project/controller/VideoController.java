package rs.ac.uns.ftn.isa.isa_project.controller;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoResponseDTO;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoUploadDTO;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.service.VideoService;
import rs.ac.uns.ftn.isa.isa_project.service.LikeService;
import rs.ac.uns.ftn.isa.isa_project.service.CommentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;

    /**
     * Endpoint za upload videa.
     * Koristi @ModelAttribute jer MultipartForm podaci ne idu u @RequestBody.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')") // Samo ulogovani korisnici mogu da uploaduju
    public ResponseEntity<VideoResponseDTO> uploadVideo(@ModelAttribute @Valid VideoUploadDTO dto) {
        try {
            // Pozivamo servis koji radi transakciju i čuvanje na disk
            Video savedVideo = videoService.createVideo(dto);
            VideoResponseDTO response = new VideoResponseDTO(savedVideo);

            response.setLikeCount(likeService.getLikeCount(savedVideo.getId()));
            response.setCommentCount(commentService.getCommentCount(savedVideo.getId()));
            // Pakujemo u ResponseDTO (prema Claude-ovom modelu) da ne bismo otkrili putanje na disku
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Ako se desi greška (npr. disk pun ili transakcija pukne), vraćamo 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint za preuzimanje thumbnail-a sa keširanjem.
     */
    @GetMapping(value = "/{id}/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        try {
            byte[] image = videoService.getThumbnailContent(id);
            return ResponseEntity.ok(image);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
    // GET detalji videa po ID-u
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDTO> getVideo(@PathVariable Long id) {
        try {
            Video video = videoService.getVideoById(id);
            VideoResponseDTO response = new VideoResponseDTO(video);

            response.setLikeCount(likeService.getLikeCount(id));
            response.setCommentCount(commentService.getCommentCount(id));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET lista svih videa
    @GetMapping
    public ResponseEntity<List<VideoResponseDTO>> getAllVideos() {
        List<Video> videos = videoService.getAllVideos();
        List<VideoResponseDTO> dtoList = videos.stream()
                .map(video -> {
                    VideoResponseDTO dto = new VideoResponseDTO(video);
                    dto.setLikeCount(likeService.getLikeCount(video.getId()));
                    dto.setCommentCount(commentService.getCommentCount(video.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
    @GetMapping(
            value = "/{id}/stream",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Resource> streamVideo(@PathVariable Long id) throws Exception {
        Video video = videoService.getVideoById(id);

        Path path = Paths.get(video.getVideoPath());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id) {
        try {
            videoService.incrementViewCount(id);
            return ResponseEntity.ok().build();  // 200 OK
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();  // 404 Not Found
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();  // 500
        }
    }
}