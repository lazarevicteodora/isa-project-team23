package rs.ac.uns.ftn.isa.isa_project;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.UserRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.VideoService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class VideoConcurrencyTest {

    private static final Logger logger = LoggerFactory.getLogger(VideoConcurrencyTest.class);

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testVideoConcurrency() throws InterruptedException {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("███ CONCURRENCY TEST STARTS ███");
        System.out.println("=".repeat(80) + "\n");
        logger.info("🚀 Starting video concurrency test");

        // Create or get test user
        User testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User user = new User();
            user.setUsername("test-user-concurrency");
            user.setEmail("test-concurrency@test.com");
            user.setPassword("test123");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setActivated(true);
            user.setEnabled(true);
            return userRepository.save(user);
        });

        System.out.println("👤 Test user: " + testUser.getUsername() + " (ID: " + testUser.getId() + ")");
        logger.info("Using test user: {} (ID: {})", testUser.getUsername(), testUser.getId());

        // Create test video
        Video testVideo = new Video();
        testVideo.setTitle("Test Video for Concurrency");
        testVideo.setDescription("Testing concurrent view increments");
        testVideo.setVideoPath("test/video.mp4");
        testVideo.setThumbnailPath("test/thumb.jpg");
        testVideo.setViewCount(0L);
        testVideo.setAuthor(testUser);
        testVideo = videoRepository.save(testVideo);

        System.out.println("📹 Kreiran test video:");
        System.out.println("   - ID: " + testVideo.getId());
        System.out.println("   - Title: " + testVideo.getTitle());
        System.out.println("   - Initial view count: " + testVideo.getViewCount());
        logger.info("Created test video with ID: {}, initial views: {}", testVideo.getId(), testVideo.getViewCount());

        final Long videoId = testVideo.getId();
        final int numberOfThreads = 10;
        final int incrementsPerThread = 5;
        final int expectedTotalViews = numberOfThreads * incrementsPerThread;

        System.out.println("\n📊 Test parameters:");
        System.out.println("   - Number of threads: " + numberOfThreads);
        System.out.println("   - Increments per thread: " + incrementsPerThread);
        System.out.println("   - Expected total views: " + expectedTotalViews);
        logger.info("Test parameters - Threads: {}, Increments per thread: {}, Expected total: {}",
                numberOfThreads, incrementsPerThread, expectedTotalViews);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println("\n🧵 Starting " + numberOfThreads + " threads...\n");
        logger.info("Starting {} threads for concurrent view increments", numberOfThreads);

        // Pokreni niti
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i + 1;
            executorService.submit(() -> {
                try {
                    System.out.println("🔵 [THREAD-" + threadId + "] Waiting for start signal...");
                    startLatch.await();

                    System.out.println("🟢 [THREAD-" + threadId + "] STARTED - beginning increments");
                    logger.info("Thread {} started execution", threadId);

                    for (int j = 0; j < incrementsPerThread; j++) {
                        try {
                            System.out.println("   ⚡ [THREAD-" + threadId + "] Attempting increment #" + (j + 1) + "/" + incrementsPerThread);

                            long startTime = System.currentTimeMillis();
                            videoService.incrementViewCount(videoId);
                            long duration = System.currentTimeMillis() - startTime;

                            successCount.incrementAndGet();
                            System.out.println("   ✅ [THREAD-" + threadId + "] SUCCESS increment #" + (j + 1) + " (duration: " + duration + "ms)");
                            logger.info("Thread {} successfully incremented views (attempt {}/{}), took {}ms",
                                    threadId, j + 1, incrementsPerThread, duration);

                            Thread.sleep(10);

                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            System.err.println("   ❌ [THREAD-" + threadId + "] ERROR on increment #" + (j + 1) + ": " + e.getMessage());
                            logger.error("Thread {} failed to increment views (attempt {}/{}): {}",
                                    threadId, j + 1, incrementsPerThread, e.getMessage());
                        }
                    }

                    System.out.println("🏁 [THREAD-" + threadId + "] FINISHED");
                    logger.info("Thread {} completed all increments", threadId);

                } catch (InterruptedException e) {
                    System.err.println("❌ [THREAD-" + threadId + "] Interrupted: " + e.getMessage());
                    logger.error("Thread {} was interrupted", threadId, e);
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        System.out.println("\n⏱️  All threads ready, sending start signal for simultaneous execution...\n");
        Thread.sleep(100);
        startLatch.countDown();

        System.out.println("⏳ Waiting for all threads to finish...\n");
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        if (!finished) {
            System.err.println("⚠️  WARNING: Some threads did not finish in time!");
            logger.warn("Some threads did not finish in time");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📈 TEST RESULTS");
        System.out.println("=".repeat(80));

        Video updatedVideo = videoRepository.findById(videoId).orElseThrow();

        System.out.println("Successful increments: " + successCount.get() + "/" + expectedTotalViews);
        System.out.println("Failed increments: " + failureCount.get());
        System.out.println("Final view count in DB: " + updatedVideo.getViewCount());
        System.out.println("Expected view count: " + expectedTotalViews);

        logger.info("Test results - Successful: {}, Failed: {}, Final views: {}, Expected: {}",
                successCount.get(), failureCount.get(), updatedVideo.getViewCount(), expectedTotalViews);

        if (updatedVideo.getViewCount() == expectedTotalViews) {
            System.out.println("\n✅ TEST PASSED - View count is correct!");
            System.out.println("=".repeat(80) + "\n");
        } else {
            System.out.println("\n❌ TEST FAILED - View count does not match expected!");
            System.out.println("   Difference: " + (expectedTotalViews - updatedVideo.getViewCount()) + " views");
            System.out.println("=".repeat(80) + "\n");
        }

        assertEquals(0, failureCount.get(), "There should be no failed attempts");
        assertEquals(expectedTotalViews, successCount.get(), "All attempts must be successful");
        assertEquals(expectedTotalViews, updatedVideo.getViewCount(), "View count must be correct");

        assertTrue(updatedVideo.getViewCount() > 0, "View count must be greater than zero");

        logger.info("✅ Video concurrency test completed successfully");
    }
}