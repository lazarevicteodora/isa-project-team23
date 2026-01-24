package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final String STORAGE_DIR = "storage/";

    /**
     * Sinhronizovano čuvanje fajla
     */
    public String saveFile(MultipartFile file, String subfolder) throws IOException {
        System.out.println("=== SAVE FILE START ===");
        System.out.println("Original filename: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize());
        System.out.println("Content type: " + file.getContentType());

        String projectPath = System.getProperty("user.dir");
        System.out.println("Project path: " + projectPath);

        Path storageDirectory = Paths.get(projectPath, STORAGE_DIR, subfolder);
        System.out.println("Storage directory: " + storageDirectory);

        Files.createDirectories(storageDirectory);
        System.out.println("Directory created/exists: " + Files.exists(storageDirectory));

        String uniqueFilename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        Path targetPath = storageDirectory.resolve(uniqueFilename);
        System.out.println("Target path: " + targetPath);

        try {
            file.transferTo(targetPath.toFile());
            System.out.println("✅ File saved successfully!");
            logger.info("Fajl sačuvan: {}", targetPath);
            return targetPath.toString();
        } catch (Exception e) {
            System.err.println("❌ TRANSFER FAILED!");
            e.printStackTrace();
            throw e;
        }
    }
    /**
     * Asinhrono čuvanje fajla (za video sa timeout-om)
     */
    @Async
    public CompletableFuture<String> saveFileAsync(MultipartFile file, String subfolder) {
        try {
            // SIMULACIJA DUGOG UPLOAD-A - odkomentiraj za testiranje!
            //Thread.sleep(Duration.ofSeconds(15000).toMillis());

            String path = saveFile(file, subfolder);
            return CompletableFuture.completedFuture(path);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Čuva video sa timeout-om
     */
    public String saveVideoWithTimeout(MultipartFile file, String subfolder, long timeout, TimeUnit unit) throws Exception {
        try {
            logger.info("Pokrenut upload videa sa timeout-om: {} {}", timeout, unit);
            return saveFileAsync(file, subfolder).get(timeout, unit);
        } catch (TimeoutException e) {
            logger.error("Upload videa prekoračio vreme!");
            throw new Exception("Upload videa prekoračio " + timeout + " " + unit);
        }
    }

    /**
     * Briše fajl sa diska
     */
    public boolean deleteFile(String path) {
        try {
            return Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            logger.error("Greška pri brisanju fajla: {}", path);
            return false;
        }
    }

    /**
     * Čita fajl sa diska
     */
    public byte[] readFile(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}
