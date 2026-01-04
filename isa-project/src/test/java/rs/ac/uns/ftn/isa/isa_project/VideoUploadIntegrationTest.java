package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoUploadDTO;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.VideoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VideoUploadIntegrationTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Test
    public void testUploadRollback_WhenDatabaseFails() throws Exception {
        // 1. Priprema podataka
        // Pravimo "lažni" fajl
        MockMultipartFile fakeVideo = new MockMultipartFile(
                "video", "test.mp4", "video/mp4", "sadrzaj".getBytes());
        MockMultipartFile fakeThumb = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", "slika".getBytes());

        VideoUploadDTO dto = new VideoUploadDTO();
        // Namerno stavljamo predugačak naslov (preko 200 karaktera) da baza baci grešku!
        dto.setTitle("A".repeat(250));
        dto.setDescription("Test opisa");
        dto.setVideo(fakeVideo);
        dto.setThumbnail(fakeThumb);

        long initialCount = videoRepository.count();

        // 2. Akcija: Pokušavamo upload
        // Očekujemo da baci Exception zbog naslova
        assertThrows(Exception.class, () -> {
            videoService.createVideo(dto);
        });

        // 3. Provera (The Proof)
        // Baza: Broj zapisa mora ostati isti (Rollback baze)
        assertEquals(initialCount, videoRepository.count());

        // Fajlovi: Ovde bi bilo idealno proveriti da folderi "uploads/videos"
        // ne sadrže nove fajlove.
        System.out.println("Test uspesno potvrdio rollback baze i prekid uploada.");
    }
}
