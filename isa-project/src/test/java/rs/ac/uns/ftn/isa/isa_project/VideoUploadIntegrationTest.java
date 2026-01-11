package rs.ac.uns.ftn.isa.isa_project;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.isa.isa_project.dto.VideoUploadDTO;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.VideoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VideoUploadIntegrationTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        // Cleanup
        videoRepository.deleteAll();
        userRepository.deleteAll();

        // Kreiraj test korisnika
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@isa.com");
        testUser.setPassword("pass123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Adresa 1");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    /**
     * TEST 1: Rollback zbog greške u bazi (predugačak naslov)
     */
    @Test
    @WithMockUser(username = "testuser")
    public void testUploadRollback_WhenDatabaseFails() {
        // 1. Priprema podataka
        MockMultipartFile fakeVideo = new MockMultipartFile(
                "video", "test.mp4", "video/mp4", "video sadrzaj".getBytes());
        MockMultipartFile fakeThumb = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", "slika sadrzaj".getBytes());

        VideoUploadDTO dto = new VideoUploadDTO();
        // NAMERNO predugačak naslov (preko 200 karaktera)
        dto.setTitle("A".repeat(250)); // 250 karaktera - predugo!
        dto.setDescription("Test opis");
        dto.setVideo(fakeVideo);
        dto.setThumbnail(fakeThumb);

        long initialCount = videoRepository.count();

        // 2. Akcija - očekujemo Exception
        Exception exception = assertThrows(Exception.class, () -> {
            videoService.createVideo(dto);
        });

        // 3. Provera
        // Baza: Broj zapisa mora ostati isti (Rollback baze)
        assertEquals(initialCount, videoRepository.count(),
                "Video ne bi trebalo da bude sačuvan u bazi!");

        // Poruka greške
        assertNotNull(exception.getMessage());
        System.out.println("✅ TEST 1 PROŠAO: Rollback zbog database greške - OK");
    }


    /**
     * TEST 3: Rollback zbog validacione greške (loš format fajla)
     * Ovaj test simulira šta se dešava kad korisnik pokuša da upload-uje
     * fajl koji nije MP4
     */
    @Test
    @WithMockUser(username = "testuser")
    public void testUploadRollback_WhenInvalidFileFormat() {
        // 1. Priprema - video koji NIJE .mp4
        MockMultipartFile invalidVideo = new MockMultipartFile(
                "video", "test.avi", "video/avi", "avi video".getBytes());
        MockMultipartFile validThumb = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", "slika".getBytes());

        VideoUploadDTO dto = new VideoUploadDTO();
        dto.setTitle("Test naslov");
        dto.setDescription("Test opis");
        dto.setVideo(invalidVideo);
        dto.setThumbnail(validThumb);

        long initialCount = videoRepository.count();


        try {
            videoService.createVideo(dto);
            fail("Trebalo bi da baci Exception zbog lošeg formata!");
        } catch (Exception e) {
            // Očekivano - validacija bi trebalo da spreči upload
        }

        // 3. Provera - video ne bi trebalo da bude u bazi
        assertEquals(initialCount, videoRepository.count());

        System.out.println("✅ TEST 3 PROŠAO: Rollback zbog lošeg formata - OK");
    }

}