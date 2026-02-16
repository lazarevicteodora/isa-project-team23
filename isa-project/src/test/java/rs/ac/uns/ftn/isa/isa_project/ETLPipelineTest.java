package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.*;
import rs.ac.uns.ftn.isa.isa_project.repository.*;
import rs.ac.uns.ftn.isa.isa_project.service.ETLPipelineService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ETLPipelineTest {

    @Autowired
    private ETLPipelineService etlPipelineService;

    @Autowired
    private ViewLogRepository viewLogRepository;

    @Autowired
    private PopularVideoResultRepository popularVideoResultRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Video video1, video2, video3, video4;

    @BeforeEach
    public void setUp() {
        popularVideoResultRepository.deleteAll();
        viewLogRepository.deleteAll();
        videoRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        testUser = new User();
        testUser.setEmail("etl_test@test.com");
        testUser.setUsername("etl_test_user");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("ETL");
        testUser.setLastName("Tester");
        testUser.setAddress("Test Street 1");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singletonList(userRole));
        testUser = userRepository.save(testUser);

        video1 = createVideo("Video 1 - Najpopularniji");
        video2 = createVideo("Video 2 - Drugi");
        video3 = createVideo("Video 3 - Treci");
        video4 = createVideo("Video 4 - Cetrvti");
    }

    private Video createVideo(String title) {
        Video v = new Video();
        v.setTitle(title);
        v.setDescription("Opis za " + title);
        v.setAuthor(testUser);
        v.setVideoPath("test/video.mp4");
        v.setThumbnailPath("test/thumb.jpg");
        v.setViewCount(0L);
        v.setTags(new HashSet<>());
        return videoRepository.save(v);
    }

    private void addViewLog(Video video, int daysAgo, long count) {
        LocalDate date = LocalDate.now().minusDays(daysAgo);
        ViewLog log = new ViewLog(video, date);
        log.setViewCount(count);
        viewLogRepository.save(log);
    }

    /**
     * TEST 1: Pipeline se uspešno pokreće i upisuje rezultate u bazu.
     */
    @Test
    public void testPipelineRunsAndSavesResults() {
        // Ubaci view logove za video1
        addViewLog(video1, 1, 100L); // juče: weight=7 -> 700
        addViewLog(video2, 1, 50L);  // juče: weight=7 -> 350
        addViewLog(video3, 1, 20L);  // juče: weight=7 -> 140

        etlPipelineService.runPipeline();

        List<PopularVideoResult> results = popularVideoResultRepository.findLatestResults();
        assertFalse(results.isEmpty(), "Rezultati ne smeju biti prazni");
        assertEquals(3, results.size(), "Treba da bude tačno 3 rezultata");
    }

    /**
     * TEST 2: Provera da je popularity score ispravno izračunat.
     *
     * Primer:
     * - video1: 100 pregleda od pre 1 dan -> weight=7 -> score = 700
     * - video2: 100 pregleda od pre 7 dana -> weight=1 -> score = 100
     */
    @Test
    public void testPopularityScoreCalculation() {
        addViewLog(video1, 1, 100L); // score = 100 * 7 = 700
        addViewLog(video2, 7, 100L); // score = 100 * 1 = 100

        etlPipelineService.runPipeline();

        List<PopularVideoResult> results = popularVideoResultRepository.findLatestResults();

        PopularVideoResult rank1 = results.stream()
                .filter(r -> r.getRankPosition() == 1)
                .findFirst()
                .orElseThrow();

        PopularVideoResult rank2 = results.stream()
                .filter(r -> r.getRankPosition() == 2)
                .findFirst()
                .orElseThrow();

        assertEquals(video1.getId(), rank1.getVideo().getId(),
                "Video1 treba da bude na 1. mestu (ima novije preglede)");
        assertEquals(700.0, rank1.getPopularityScore(), 0.01,
                "Score za video1 treba da bude 700");

        assertEquals(video2.getId(), rank2.getVideo().getId(),
                "Video2 treba da bude na 2. mestu");
        assertEquals(100.0, rank2.getPopularityScore(), 0.01,
                "Score za video2 treba da bude 100");
    }

    /**
     * TEST 3: Pipeline čuva samo top 3, čak i ako ima više videa.
     */
    @Test
    public void testOnlyTop3AreSaved() {
        addViewLog(video1, 1, 100L);
        addViewLog(video2, 1, 80L);
        addViewLog(video3, 1, 60L);
        addViewLog(video4, 1, 40L); // ovaj ne treba da bude u top 3

        etlPipelineService.runPipeline();

        List<PopularVideoResult> results = popularVideoResultRepository.findLatestResults();
        assertEquals(3, results.size(), "Treba da bude sačuvana tačno 3 videa");

        boolean video4Present = results.stream()
                .anyMatch(r -> r.getVideo().getId().equals(video4.getId()));
        assertFalse(video4Present, "Video4 ne sme biti u top 3");
    }

    /**
     * TEST 4: Weight formula - recentiji pregledi imaju veći uticaj.
     *
     * video1: 10 pregleda juče (daysAgo=1) -> weight=7 -> score=70
     * video2: 50 pregleda pre 7 dana (daysAgo=7) -> weight=1 -> score=50
     *
     * video1 mora biti ispred video2 uprkos manjem broju pregleda.
     */
    @Test
    public void testRecentViewsHaveHigherWeight() {
        addViewLog(video1, 1, 10L);  // score = 10 * 7 = 70
        addViewLog(video2, 7, 50L);  // score = 50 * 1 = 50

        etlPipelineService.runPipeline();

        List<PopularVideoResult> results = popularVideoResultRepository.findLatestResults();

        PopularVideoResult rank1 = results.stream()
                .filter(r -> r.getRankPosition() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(video1.getId(), rank1.getVideo().getId(),
                "Video sa recentnijim pregledima treba da bude na 1. mestu");
    }

    /**
     * TEST 5: getLatestPopularVideos() vraća rezultate iz poslednjeg pokretanja.
     *
     * Pokrećemo pipeline 2 puta - drugi put treba da prepiše prvi.
     */
    @Test
    @Transactional
    public void testGetLatestReturnsNewestRun() throws InterruptedException {
        addViewLog(video1, 1, 100L);
        addViewLog(video2, 1, 50L);
        addViewLog(video3, 1, 20L);

        etlPipelineService.runPipeline();

        // Sačekaj 1 sekundu da se razlikuje runAt timestamp
        Thread.sleep(1000);

        // Promeni podatke i ponovo pokreni
        viewLogRepository.deleteAll();
        addViewLog(video4, 1, 999L); // sad je video4 najpopularniji
        addViewLog(video1, 1, 10L);
        addViewLog(video2, 1, 5L);

        etlPipelineService.runPipeline();

        List<PopularVideoResult> latest = etlPipelineService.getLatestPopularVideos();

        PopularVideoResult rank1 = latest.stream()
                .filter(r -> r.getRankPosition() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(video4.getId(), rank1.getVideo().getId(),
                "Posle drugog pokretanja, video4 treba da bude #1");
    }

    /**
     * TEST 6: Ako nema view logova, pipeline ne baca grešku i lista je prazna.
     */
    @Test
    public void testPipelineWithNoData() {
        assertDoesNotThrow(() -> etlPipelineService.runPipeline());

        List<PopularVideoResult> results = etlPipelineService.getLatestPopularVideos();
        assertTrue(results.isEmpty(), "Bez podataka, lista treba da bude prazna");
    }

    /**
     * TEST 7: Pregledi stariji od 7 dana ne utiču na score.
     */
    @Test
    public void testOldViewsAreIgnored() {
        addViewLog(video1, 8, 1000L); // stariji od 7 dana - ne računa se
        addViewLog(video2, 1, 1L);    // juče, samo 1 pregled

        etlPipelineService.runPipeline();

        List<PopularVideoResult> results = popularVideoResultRepository.findLatestResults();

        // video2 treba da bude #1 jer video1 nema validnih pregleda
        PopularVideoResult rank1 = results.stream()
                .filter(r -> r.getRankPosition() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(video2.getId(), rank1.getVideo().getId(),
                "Video sa pregledima starijim od 7 dana treba da bude zanemaren");
    }
}