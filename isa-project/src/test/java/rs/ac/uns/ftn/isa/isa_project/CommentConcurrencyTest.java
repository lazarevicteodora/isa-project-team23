package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.isa.isa_project.model.Role;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.CommentRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.RoleRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.CommentService;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CommentConcurrencyTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Video testVideo;

    @BeforeEach
    public void setUp() throws Exception {
        commentRepository.deleteAll();
        videoRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // User
        testUser = new User();
        testUser.setEmail("comment_test@test.com");
        testUser.setUsername("comment_test_user");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Comment");
        testUser.setLastName("Tester");
        testUser.setAddress("Test Street 1");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singletonList(userRole));
        testUser = userRepository.save(testUser);

        // Video - minimalne putanje za test
        testVideo = new Video();
        testVideo.setTitle("Comment Test Video");
        testVideo.setDescription("Video for testing comments");
        testVideo.setAuthor(testUser);
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setThumbnailPath("test/thumb.jpg");
        testVideo.setViewCount(0L);
        testVideo.setTags(new HashSet<>());
        testVideo = videoRepository.save(testVideo);
    }

    @Test
    public void testConcurrentComments() throws InterruptedException {
        int totalAttempts = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(totalAttempts);
        CountDownLatch latch = new CountDownLatch(totalAttempts);

        for (int i = 0; i < totalAttempts; i++) {
            final int commentNum = i;
            executor.execute(() -> {
                try {
                    commentService.addComment(
                            testVideo.getId(),
                            "Concurrent comment #" + commentNum,
                            testUser
                    );
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    if (e.getMessage().contains("exceeded")) {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long dbCommentCount = commentRepository.countByVideoId(testVideo.getId());

        System.out.println("TEST ZAVRŠEN. Success: " + successCount.get() + ", Failures: " + failureCount.get());
        System.out.println("DB Comment Count: " + dbCommentCount);

        assertEquals(60, successCount.get(), "Should allow exactly 60 comments");
        assertEquals(40, failureCount.get(), "Should block exactly 40 comments");
        assertEquals(60, dbCommentCount, "Database should have exactly 60 comments");
    }
}