package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.isa.isa_project.model.Comment;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CommentRateLimitTest {

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
        // Cleanup - isto kao kod video testova
        commentRepository.deleteAll();
        videoRepository.deleteAll();
        userRepository.deleteAll();

        // Role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // User
        testUser = new User();
        testUser.setEmail("ratelimit_test@test.com");
        testUser.setUsername("ratelimit_test_user");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("RateLimit");
        testUser.setLastName("Tester");
        testUser.setAddress("Test Street 2");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser.setRoles(Collections.singletonList(userRole));
        testUser = userRepository.save(testUser);

        // Video - minimalne putanje za test
        testVideo = new Video();
        testVideo.setTitle("RateLimit Test Video");
        testVideo.setDescription("Video for testing rate limits");
        testVideo.setAuthor(testUser);
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setThumbnailPath("test/thumb.jpg");
        testVideo.setViewCount(0L);
        testVideo.setTags(new HashSet<>());
        testVideo = videoRepository.save(testVideo);
    }

    @Test
    public void testCommentRateLimitExceeded() {
        int successfulComments = 0;

        for (int i = 0; i < 65; i++) {
            try {
                Comment comment = commentService.addComment(
                        testVideo.getId(),
                        "Test comment #" + i,
                        testUser
                );
                assertNotNull(comment);
                successfulComments++;
            } catch (RuntimeException e) {
                if (!e.getMessage().contains("exceeded")) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }
        }

        System.out.println("Successful comments: " + successfulComments);

        assertEquals(60, successfulComments, "Should allow exactly 60 comments per hour");

        long dbCommentCount = commentRepository.countByVideoId(testVideo.getId());
        assertEquals(60, dbCommentCount, "Database should contain exactly 60 comments");
    }

    @Test
    public void testRateLimitIsPerUser() throws Exception {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        // Drugi korisnik
        User secondUser = new User();
        secondUser.setEmail("second_user@test.com");
        secondUser.setUsername("second_user");
        secondUser.setPassword(passwordEncoder.encode("password123"));
        secondUser.setFirstName("Second");
        secondUser.setLastName("User");
        secondUser.setAddress("Test Street 3");
        secondUser.setActivated(true);
        secondUser.setEnabled(true);
        secondUser.setRoles(Collections.singletonList(userRole));
        final User finalSecondUser = userRepository.save(secondUser);

        // Prvi korisnik kreira 60 komentara
        for (int i = 0; i < 60; i++) {
            commentService.addComment(testVideo.getId(), "User1 comment #" + i, testUser);
        }

        // Prvi korisnik bi trebalo da bude blokiran
        assertThrows(RuntimeException.class, () -> {
            commentService.addComment(testVideo.getId(), "User1 comment #61", testUser);
        });

        // Drugi korisnik može da komentariše
        assertDoesNotThrow(() -> {
            Comment comment = commentService.addComment(
                    testVideo.getId(),
                    "User2 comment #1",
                    finalSecondUser
            );
            assertNotNull(comment);
        });

        long totalComments = commentRepository.countByVideoId(testVideo.getId());
        System.out.println("Total comments in DB: " + totalComments);
        assertEquals(61, totalComments);
    }
}