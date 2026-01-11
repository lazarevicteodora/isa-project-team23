package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rs.ac.uns.ftn.isa.isa_project.dto.VideoUploadDTO;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Video createVideo(VideoUploadDTO dto) throws Exception {
        User author = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<String> uploadedFiles = new ArrayList<>();

        try {
            // 1. Upload thumbnail-a
            String thumbnailPath = fileStorageService.saveFile(dto.getThumbnail(), "thumbs");
            uploadedFiles.add(thumbnailPath);

            // 2. Upload videa SA TIMEOUT-OM (5 minuta)
            String videoPath = fileStorageService.saveVideoWithTimeout(
                    dto.getVideo(),
                    "videos",
                    5,  // 5 minuta timeout
                    TimeUnit.MINUTES
            );
            uploadedFiles.add(videoPath);

            // 3. Kreiranje Video objekta
            Video video = new Video();
            video.setTitle(dto.getTitle());
            video.setDescription(dto.getDescription());
            video.setTags(dto.getParsedTags());
            video.setLatitude(dto.getLatitude());
            video.setLongitude(dto.getLongitude());
            video.setAuthor(author);
            video.setVideoPath(videoPath);
            video.setThumbnailPath(thumbnailPath);
            // 4. Čuvanje u bazi
            return videoRepository.save(video);

        } catch (TimeoutException e) {
            // Timeout - ROLLBACK FAJLOVA!
            logger.error("TIMEOUT! Pokrećem rollback fajlova...");
            rollbackFiles(uploadedFiles);
            throw e; // Prosleđujemo dalje u Controller

        } catch (IOException e) {
            // I/O greška - ROLLBACK FAJLOVA!
            logger.error("I/O GREŠKA! Pokrećem rollback fajlova...");
            rollbackFiles(uploadedFiles);
            throw e; // Prosleđujemo dalje

        } catch (Exception e) {
            // Bilo koja druga greška (DB, validacija) - ROLLBACK FAJLOVA!
            logger.error("GREŠKA! Pokrećem rollback fajlova: {}", e.getMessage());
            rollbackFiles(uploadedFiles);
            throw new Exception("Kreiranje videa neuspelo: " + e.getMessage(), e);
        }
    }
    private void rollbackFiles(List<String> uploadedFiles) {
        for (String filePath : uploadedFiles) {
            boolean deleted = fileStorageService.deleteFile(filePath);
            if (deleted) {
                logger.info("Fajl obrisan: {}", filePath);
            } else {
                logger.warn("Nije moguće obrisati fajl: {}", filePath);
            }
        }
    }
    @Override
    @Cacheable(value = "thumbnails", key = "#videoId")
    public byte[] getThumbnailContent(Long videoId) throws Exception {
        // Ova metoda se izvršava SAMO ako slika nije u kešu
        Video v = videoRepository.findById(videoId)
                .orElseThrow(() -> new Exception("Video nije pronađen"));

        Path path = Paths.get(v.getThumbnailPath());
        System.out.println("ČITAM THUMBNAIL SA DISKA");
        return Files.readAllBytes(path);
    }

    @Override
    @Transactional(readOnly = true)
    public Video getVideoById(Long videoId) throws Exception {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new Exception("Video nije pronađen"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc();
    }

    // Increment SA TRANSAKCIJOM i LOCK-om
    @Override
    @Transactional(timeout = 5)
    public void incrementViewCount(Long videoId) {
        // 1. Pročitaj video SA LOCK-om (drugi korisnici ČEKAJU ovde)
        Video video = videoRepository.findByIdForUpdate(videoId)
                .orElseThrow(() -> new RuntimeException("Video ne postoji!"));

        // 2. Incrementuj brojač
        video.setViewCount(video.getViewCount() + 1);

        // 3. Sačuvaj (automatski se čuva zbog @Transactional)
        videoRepository.save(video);

        // Lock se oslobađa na kraju metode (na kraju transakcije)
    }
}