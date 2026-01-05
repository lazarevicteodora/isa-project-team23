package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.VideoService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class VideoConcurrencyTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        // Čistimo bazu pre testa da podaci ne bi ostajali i pravili greške sa brojevima
        // Prvo brišemo videe, pa korisnike zbog stranih ključeva
        videoRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSimultaniPregledi() throws InterruptedException {
        // 1. Kreiramo novog korisnika
        User autor = new User();
        autor.setUsername("test_user_" + System.currentTimeMillis());
        autor.setEmail("test@isa.com");
        autor.setPassword("sifra123");
        autor.setFirstName("Petar");
        autor.setLastName("Petrovic");
        autor.setAddress("Bulevar Oslobodjenja 1");
        autor.setActivated(true);
        autor.setEnabled(true);
        userRepository.save(autor);

        // 2. Kreiramo video sa 0 pregleda
        Video video = new Video();
        video.setTitle("Test Video");
        video.setAuthor(autor);
        video.setVideoPath("putanja/video.mp4");
        video.setThumbnailPath("putanja/slika.jpg");
        video.setViewCount(0L);
        video = videoRepository.save(video);

        Long videoId = video.getId();

        // 3. Simuliramo 20 paralelnih klikova
        int brojKorisnika = 20;
        ExecutorService executor = Executors.newFixedThreadPool(brojKorisnika);
        CountDownLatch latch = new CountDownLatch(brojKorisnika);

        for (int i = 0; i < brojKorisnika; i++) {
            executor.execute(() -> {
                try {
                    videoService.incrementViewCount(videoId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 4. Provera rezultata
        Video osvezenVideo = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video nije pronađen"));

        System.out.println("TEST ZAVRŠEN. BROJ PREGLEDA JE: " + osvezenVideo.getViewCount());

        assertEquals(Long.valueOf(brojKorisnika), osvezenVideo.getViewCount(),
                "Greška: Broj pregleda u bazi nije tačno 20!");
    }
}