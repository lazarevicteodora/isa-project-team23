package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingJob;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingStatus;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.TranscodingJobRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.TranscodingProducer;
import rs.ac.uns.ftn.isa.isa_project.service.TranscodingService;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test za transcoding funkcionalnost.
 *
 * TESTIRA:
 * 1. Kreiranje transcoding job-a
 * 2. Slanje poruke u RabbitMQ queue
 * 3. Persistovanje job-a u bazu
 * 4. Multiple job-ovi za multiple videa
 * 5. Retry funkcionalnost
 */
@SpringBootTest
@ActiveProfiles("test")
public class TranscodingIntegrationTest {

    @Autowired
    private TranscodingProducer transcodingProducer;

    @Autowired
    private TranscodingService transcodingService;

    @Autowired
    private TranscodingJobRepository transcodingJobRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * TEST 1: Kreiranje transcoding job-a
     *
     * Proverava:
     * - Da li se job kreira sa PENDING statusom
     * - Da li se čuva u bazi
     * - Da li je povezan sa pravim video-om
     */
    @Test
    public void testTranscodingJobCreation() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 TEST 1: Transcoding Job Creation");
        System.out.println("=".repeat(80));

        // 1. Kreiraj test usera
        User testUser = new User();
        testUser.setEmail("transcoding_test@test.com");
        testUser.setUsername("transcoding_user");
        testUser.setPassword("password123");
        testUser.setFirstName("Transcoding");
        testUser.setLastName("Tester");
        testUser.setAddress("Test Street 1");
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        System.out.println("✅ Test user created: " + testUser.getUsername());

        // 2. Kreiraj test video
        Video testVideo = new Video();
        testVideo.setTitle("Transcoding Test Video");
        testVideo.setDescription("Video for transcoding test");
        testVideo.setAuthor(testUser);
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setThumbnailPath("test/thumb.jpg");
        testVideo.setViewCount(0L);
        testVideo.setTags(new HashSet<>());
        testVideo = videoRepository.save(testVideo);

        System.out.println("✅ Test video created: ID=" + testVideo.getId());

        // 3. Pošalji video na transcoding
        String jobId = transcodingProducer.sendVideoForTranscoding(testVideo);

        assertNotNull(jobId, "Job ID should not be null");
        System.out.println("✅ Transcoding job created: " + jobId);

        // 4. Proveri da li je job kreiran u bazi
        TranscodingJob job = transcodingJobRepository.findByJobId(jobId).orElse(null);

        assertNotNull(job, "TranscodingJob should be saved in database");
        assertEquals(TranscodingStatus.PENDING, job.getStatus(), "Initial status should be PENDING");
        assertEquals(testVideo.getId(), job.getVideo().getId(), "Video ID should match");

        System.out.println("✅ Job status: " + job.getStatus());
        System.out.println("✅ Job video ID: " + job.getVideo().getId());

        // 5. Proveri da li job postoji u listi job-ova za video
        List<TranscodingJob> videoJobs = transcodingService.getJobsForVideo(testVideo.getId());

        assertFalse(videoJobs.isEmpty(), "Video should have at least one transcoding job");
        assertTrue(videoJobs.stream().anyMatch(j -> j.getJobId().equals(jobId)),
                "Job should be in the video's job list");

        System.out.println("✅ Video has " + videoJobs.size() + " transcoding job(s)");

        System.out.println("\n=== TEST 1 PASSED ===\n");
    }

    /**
     * TEST 2: Multiple transcoding jobs
     *
     * Proverava:
     * - Da li se može kreirati više job-ova odjednom
     * - Da li su svi job-ovi u PENDING stanju
     * - Da li statistika vraća pravilan broj pending job-ova
     */
    @Test
    public void testMultipleTranscodingJobs() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 TEST 2: Multiple Transcoding Jobs");
        System.out.println("=".repeat(80));

        // Kreiraj user
        User testUser = userRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail("multi_test@test.com");
                    user.setUsername("multi_user");
                    user.setPassword("password123");
                    user.setFirstName("Multi");
                    user.setLastName("Tester");
                    user.setAddress("Test Street 2");
                    user.setActivated(true);
                    user.setEnabled(true);
                    return userRepository.save(user);
                });

        System.out.println("✅ Using user: " + testUser.getUsername());

        // Kreiraj 3 videa i pošalji ih na transcoding
        int videoCount = 3;
        String[] jobIds = new String[videoCount];

        for (int i = 0; i < videoCount; i++) {
            Video video = new Video();
            video.setTitle("Test Video " + (i + 1));
            video.setDescription("Multi-job test video");
            video.setAuthor(testUser);
            video.setVideoPath("test/video" + i + ".mp4");
            video.setThumbnailPath("test/thumb" + i + ".jpg");
            video.setViewCount(0L);
            video.setTags(new HashSet<>());
            video = videoRepository.save(video);

            jobIds[i] = transcodingProducer.sendVideoForTranscoding(video);
            System.out.println("✅ Job " + (i + 1) + " created: " + jobIds[i]);
        }

        // Proveri da li su svi job-ovi kreirani
        for (String jobId : jobIds) {
            TranscodingJob job = transcodingJobRepository.findByJobId(jobId).orElse(null);
            assertNotNull(job, "Job should exist in database");
            assertEquals(TranscodingStatus.PENDING, job.getStatus());
        }

        System.out.println("✅ All jobs created with PENDING status");

        // Proveri statistiku
        long pendingCount = transcodingService.getPendingJobs().size();
        System.out.println("📊 Pending jobs: " + pendingCount);

        assertTrue(pendingCount >= videoCount,
                "Should have at least " + videoCount + " pending jobs");

        System.out.println("\n=== TEST 2 PASSED ===\n");
    }

    /**
     * TEST 3: Transcoding job retry functionality
     *
     * Proverava:
     * - Da li se failed job može retry-ovati
     * - Da li se status vraća na PENDING nakon retry-a
     * - Da li se error message briše
     */
    @Test
    public void testTranscodingJobRetry() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 TEST 3: Transcoding Job Retry");
        System.out.println("=".repeat(80));

        // Kreiraj test video
        User testUser = userRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(testUser, "Test user should exist");

        Video video = new Video();
        video.setTitle("Retry Test Video");
        video.setDescription("Test retry functionality");
        video.setAuthor(testUser);
        video.setVideoPath("test/retry_video.mp4");
        video.setThumbnailPath("test/retry_thumb.jpg");
        video.setViewCount(0L);
        video.setTags(new HashSet<>());
        video = videoRepository.save(video);

        System.out.println("✅ Test video created: ID=" + video.getId());

        // Pošalji na transcoding
        String jobId = transcodingProducer.sendVideoForTranscoding(video);
        System.out.println("✅ Job created: " + jobId);

        // Simuliraj failure (ručno postavi status)
        TranscodingJob job = transcodingJobRepository.findByJobId(jobId).orElseThrow();
        job.markAsFailed("Test failure - simulated error");
        transcodingJobRepository.save(job);

        System.out.println("✅ Job marked as FAILED with error message");

        // Pokušaj retry
        assertDoesNotThrow(() -> {
            transcodingService.retryJob(jobId);
        }, "Retry should not throw exception");

        System.out.println("✅ Retry executed");

        // Proveri da li je status resetovan
        TranscodingJob retriedJob = transcodingJobRepository.findByJobId(jobId).orElseThrow();
        assertEquals(TranscodingStatus.PENDING, retriedJob.getStatus(),
                "Status should be PENDING after retry");
        assertNull(retriedJob.getErrorMessage(), "Error message should be null after retry");

        System.out.println("✅ Job status after retry: " + retriedJob.getStatus());
        System.out.println("✅ Error message cleared");

        System.out.println("\n=== TEST 3 PASSED ===\n");
    }

    /**
     * TEST 4: Get transcoding statistics
     *
     * Proverava:
     * - Da li statistika vraća tačan broj job-ova po statusu
     */
    @Test
    public void testTranscodingStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 TEST 4: Transcoding Statistics");
        System.out.println("=".repeat(80));

        // Pre testiranja, očisti stare job-ove (opciono)
        long initialTotal = transcodingJobRepository.count();
        System.out.println("📊 Initial job count: " + initialTotal);

        // Kreiraj statistiku
        TranscodingService.TranscodingStats stats = transcodingService.getStats();

        System.out.println("\n📊 TRANSCODING STATISTICS:");
        System.out.println("   Total: " + stats.total);
        System.out.println("   Pending: " + stats.pending);
        System.out.println("   Processing: " + stats.processing);
        System.out.println("   Completed: " + stats.completed);
        System.out.println("   Failed: " + stats.failed);

        // Proveri da suma svih statusa == total
        long sum = stats.pending + stats.processing + stats.completed + stats.failed;
        assertEquals(stats.total, sum, "Sum of all statuses should equal total");

        System.out.println("\n✅ Statistics sum validated: " + sum + " = " + stats.total);

        System.out.println("\n=== TEST 4 PASSED ===\n");
    }

    /**
     * TEST 5: Job not found error handling
     *
     * Proverava:
     * - Da li se baca exception kada se traži nepostojeći job
     */
    @Test
    public void testJobNotFoundError() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 TEST 5: Job Not Found Error Handling");
        System.out.println("=".repeat(80));

        String nonExistentJobId = "non-existent-job-id-12345";

        // Pokušaj retry nepostojećeg job-a
        Exception exception = assertThrows(RuntimeException.class, () -> {
            transcodingService.retryJob(nonExistentJobId);
        });

        System.out.println("✅ Exception thrown as expected: " + exception.getMessage());

        assertTrue(exception.getMessage().contains("not found") ||
                        exception.getMessage().contains("Job not found"),
                "Exception message should indicate job not found");

        System.out.println("\n=== TEST 5 PASSED ===\n");
    }
}