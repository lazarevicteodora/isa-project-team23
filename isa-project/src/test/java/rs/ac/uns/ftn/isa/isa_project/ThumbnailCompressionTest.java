package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.RoleRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.ThumbnailCompressionService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test za periodičnu kompresiju thumbnail slika.
 *
 * TESTIRA:
 * 1. Kompresiju thumbnail-a starijih od mesec dana
 * 2. Preskakanje već kompresovanih thumbnail-a
 * 3. Čuvanje putanje do kompresovane slike u bazi
 * 4. Očuvanje originalne slike (ne briše se)
 * 5. Smanjenje veličine fajla nakon kompresije
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ThumbnailCompressionTest {

    @Autowired
    private ThumbnailCompressionService compressionService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @TempDir
    Path tempDir;

    private User testUser;

    @BeforeEach
    public void setUp() {
        videoRepository.deleteAll();
        userRepository.deleteAll();

        // Kreiraj test korisnika
        testUser = new User();
        testUser.setEmail("compression_test@test.com");
        testUser.setUsername("compression_user");
        testUser.setPassword("password123");
        testUser.setFirstName("Compression");
        testUser.setLastName("Tester");
        testUser.setAddress("Test Street 1");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    /**
     * TEST 1: Kompresija thumbnail-a starijeg od mesec dana
     */
    @Test
    public void testCompressOldThumbnail() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🖼️  TEST 1: Kompresija Starog Thumbnail-a");
        System.out.println("=".repeat(80));

        // 1. Kreiraj test sliku (thumbnail)
        File testThumbnail = createTestImage(tempDir, "test_thumbnail.jpg", 800, 600);
        long originalSize = testThumbnail.length();

        System.out.println("✅ Kreirana test slika:");
        System.out.println("   Putanja: " + testThumbnail.getAbsolutePath());
        System.out.println("   Veličina: " + originalSize + " bytes (" + (originalSize / 1024) + " KB)");

        // 2. Kreiraj video PRVO bez datuma
        Video oldVideo = new Video();
        oldVideo.setTitle("Old Video for Compression Test");
        oldVideo.setDescription("Video sa starim thumbnail-om");
        oldVideo.setAuthor(testUser);
        oldVideo.setVideoPath("test/video.mp4");
        oldVideo.setThumbnailPath(testThumbnail.getAbsolutePath());
        oldVideo.setViewCount(0L);
        oldVideo.setTags(new HashSet<>());
        oldVideo = videoRepository.save(oldVideo);

        // 3. ZATIM ažuriraj datum koristeći native SQL (zaobiđi @PrePersist)
        entityManager.createNativeQuery(
                        "UPDATE videos SET created_at = :oldDate WHERE id = :videoId")
                .setParameter("oldDate", LocalDateTime.now().minusMonths(2))
                .setParameter("videoId", oldVideo.getId())
                .executeUpdate();

        // Flush i clear da se osigura da je promena primenjena
        entityManager.flush();
        entityManager.clear();

        // 4. Ponovo učitaj video da bi dobili ažurirani datum
        oldVideo = videoRepository.findById(oldVideo.getId()).orElseThrow();

        System.out.println("✅ Kreiran test video:");
        System.out.println("   ID: " + oldVideo.getId());
        System.out.println("   Datum kreiranja: " + oldVideo.getCreatedAt());
        System.out.println("   Thumbnail putanja: " + oldVideo.getThumbnailPath());

        // 5. Pokreni kompresiju
        System.out.println("\n⏳ Pokrećem kompresiju...");
        int compressedCount = compressionService.compressOldThumbnails();

        // 6. Proveri rezultate
        assertEquals(1, compressedCount, "Trebalo bi da bude kompresovan 1 thumbnail");

        // 7. Proveri da li je putanja do kompresovane slike sačuvana u bazi
        Video updatedVideo = videoRepository.findById(oldVideo.getId()).orElseThrow();
        assertNotNull(updatedVideo.getThumbnailCompressedPath(),
                "Putanja do kompresovane slike mora biti sačuvana");

        System.out.println("\n✅ Kompresija završena:");
        System.out.println("   Kompresovano thumbnails: " + compressedCount);
        System.out.println("   Kompresovana putanja: " + updatedVideo.getThumbnailCompressedPath());

        // 8. Proveri da li kompresovani fajl postoji
        File compressedFile = new File(updatedVideo.getThumbnailCompressedPath());
        assertTrue(compressedFile.exists(), "Kompresovani fajl mora postojati");

        long compressedSize = compressedFile.length();
        System.out.println("   Kompresovana veličina: " + compressedSize + " bytes (" + (compressedSize / 1024) + " KB)");

        // 9. Proveri da li je veličina smanjena
        assertTrue(compressedSize < originalSize,
                "Kompresovani fajl mora biti manji od originalnog");

        double reduction = 100.0 * (1.0 - (double) compressedSize / originalSize);
        System.out.println("   Smanjenje: " + String.format("%.1f", reduction) + "%");

        // 10. Proveri da li originalni fajl NIJE obrisan
        assertTrue(testThumbnail.exists(), "Originalni thumbnail NE SME biti obrisan!");
        System.out.println("   ✅ Originalni fajl je očuvan (nije obrisan)");

        System.out.println("\n=== TEST 1 PASSED ===\n");
    }

    /**
     * TEST 2: Preskakanje već kompresovanih thumbnail-a
     */
    @Test
    public void testSkipAlreadyCompressedThumbnail() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("⏭️  TEST 2: Preskakanje Već Kompresovanih Thumbnails");
        System.out.println("=".repeat(80));

        // 1. Kreiraj test sliku
        File testThumbnail = createTestImage(tempDir, "already_compressed.jpg", 800, 600);
        File compressedThumbnail = createTestImage(tempDir, "compressed_already_compressed.jpg", 800, 600);

        System.out.println("✅ Kreirane test slike");

        // 2. Kreiraj video koji već ima kompresovanu verziju
        Video video = new Video();
        video.setTitle("Video sa već kompresovanim thumbnail-om");
        video.setDescription("Test");
        video.setAuthor(testUser);
        video.setVideoPath("test/video.mp4");
        video.setThumbnailPath(testThumbnail.getAbsolutePath());
        video.setThumbnailCompressedPath(compressedThumbnail.getAbsolutePath()); // Već postoji!
        video.setViewCount(0L);
        video.setTags(new HashSet<>());
        video = videoRepository.save(video);

        // 3. Ažuriraj datum koristeći native SQL
        entityManager.createNativeQuery(
                        "UPDATE videos SET created_at = :oldDate WHERE id = :videoId")
                .setParameter("oldDate", LocalDateTime.now().minusMonths(2))
                .setParameter("videoId", video.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        System.out.println("✅ Kreiran video koji već ima kompresovanu verziju");

        // 4. Pokreni kompresiju
        System.out.println("\n⏳ Pokrećem kompresiju...");
        int compressedCount = compressionService.compressOldThumbnails();

        // 5. Proveri da NIJE kompresovano (preskočeno)
        assertEquals(0, compressedCount, "Ne bi trebalo da bude kompresovano ništa (već kompresovano)");

        System.out.println("\n✅ Rezultat: Thumbnail je ispravno preskočen (već je bio kompresovan)");
        System.out.println("\n=== TEST 2 PASSED ===\n");
    }

    /**
     * TEST 3: Ne kompresuje nove thumbnail-e (mlađe od mesec dana)
     */
    @Test
    public void testDoesNotCompressNewThumbnails() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🆕 TEST 3: Ne Kompresuje Nove Thumbnails");
        System.out.println("=".repeat(80));

        // 1. Kreiraj test sliku
        File testThumbnail = createTestImage(tempDir, "new_thumbnail.jpg", 800, 600);

        System.out.println("✅ Kreirana test slika");

        // 2. Kreiraj video koji je nov (mlađi od mesec dana)
        Video newVideo = new Video();
        newVideo.setTitle("Nov Video");
        newVideo.setDescription("Test");
        newVideo.setAuthor(testUser);
        newVideo.setVideoPath("test/video.mp4");
        newVideo.setThumbnailPath(testThumbnail.getAbsolutePath());
        newVideo.setCreatedAt(LocalDateTime.now().minusDays(15)); // 15 dana star (manje od mesec)
        newVideo.setViewCount(0L);
        newVideo.setTags(new HashSet<>());
        newVideo = videoRepository.save(newVideo);

        System.out.println("✅ Kreiran nov video (15 dana star)");

        // 3. Pokreni kompresiju
        System.out.println("\n⏳ Pokrećem kompresiju...");
        int compressedCount = compressionService.compressOldThumbnails();

        // 4. Proveri da NIJE kompresovano (suviše nov)
        assertEquals(0, compressedCount, "Novi video (mlađi od mesec dana) ne bi trebalo da bude kompresovan");

        System.out.println("\n✅ Rezultat: Nov thumbnail nije kompresovan (mlađi od mesec dana)");
        System.out.println("\n=== TEST 3 PASSED ===\n");
    }

    /**
     * Helper metoda za kreiranje test slike
     */
    private File createTestImage(Path directory, String filename, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Popuni sliku gradijentom (da ima dovoljno podataka)
        GradientPaint gradient = new GradientPaint(
                0, 0, Color.BLUE,
                width, height, Color.RED
        );
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, width, height);

        // Dodaj tekst
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 40));
        graphics.drawString("TEST IMAGE", 50, height / 2);

        graphics.dispose();

        // Sačuvaj sliku
        File imageFile = directory.resolve(filename).toFile();
        ImageIO.write(image, "jpg", imageFile);

        return imageFile;
    }
}