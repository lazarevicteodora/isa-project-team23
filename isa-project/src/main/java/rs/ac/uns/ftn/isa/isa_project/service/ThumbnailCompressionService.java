package rs.ac.uns.ftn.isa.isa_project.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servis za periodičnu kompresiju thumbnail slika.
 */
@Service
public class ThumbnailCompressionService {

    private static final Logger LOG = LoggerFactory.getLogger(ThumbnailCompressionService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Value("${thumbnail.compression.quality:0.5}")
    private double compressionQuality;

    @Value("${thumbnail.compression.prefix:compressed_}")
    private String compressionPrefix;

    /**
     * Kompresuje sve thumbnail slike koje su starije od mesec dana
     * i koje još nisu kompresovane.
     */
    @Transactional
    public int compressOldThumbnails() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        LOG.info("=".repeat(80));
        LOG.info("PERIODIČNA KOMPRESIJA THUMBNAILS - ZAPOČETA");
        LOG.info("=".repeat(80));
        LOG.info("Tražim video-e kreir ane pre: {}", oneMonthAgo);
        LOG.info("Kvalitet kompresije: {}%", (compressionQuality * 100));
        LOG.info("Prefiks kompresovanih fajlova: '{}'", compressionPrefix);

        // Pronađi sve video-e koji su kreirani pre mesec dana
        List<Video> oldVideos = videoRepository.findByCreatedAtBefore(oneMonthAgo);

        if (oldVideos.isEmpty()) {
            LOG.info("Nema video-a starijih od mesec dana. Kompresija se neće izvršiti.");
            LOG.info("=".repeat(80));
            return 0;
        }

        LOG.info("Pronađeno {} video-a starijih od mesec dana", oldVideos.size());
        LOG.info("-".repeat(80));

        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        long totalOriginalSize = 0;
        long totalCompressedSize = 0;

        for (Video video : oldVideos) {
            try {
                LOG.info("Obrađujem video ID={} ('{}')", video.getId(), video.getTitle());

                // Preskoči ako već ima kompresovanu verziju
                if (video.getThumbnailCompressedPath() != null
                        && !video.getThumbnailCompressedPath().isEmpty()) {

                    File compressedFile = new File(video.getThumbnailCompressedPath());
                    if (compressedFile.exists()) {
                        LOG.info("  ⏭️  Video već ima kompresovanu verziju: {}", video.getThumbnailCompressedPath());
                        skippedCount++;
                        continue;
                    } else {
                        LOG.warn("  ⚠️  Kompresovana putanja postoji u bazi ali fajl ne postoji na disku. Kompresujem ponovo.");
                    }
                }

                // Kompresuj thumbnail
                CompressionResult result = compressThumbnail(video);

                if (result != null && result.compressedPath != null) {
                    // Sačuvaj putanju do kompresovane slike u bazi
                    video.setThumbnailCompressedPath(result.compressedPath);
                    videoRepository.save(video);

                    totalOriginalSize += result.originalSize;
                    totalCompressedSize += result.compressedSize;
                    successCount++;

                    double reductionPercent = 100.0 * (1.0 - (double) result.compressedSize / result.originalSize);

                    LOG.info("  ✅ Uspešno kompresovano:");
                    LOG.info("     Original:     {} bytes ({} KB)", result.originalSize, result.originalSize / 1024);
                    LOG.info("     Kompresovano: {} bytes ({} KB)", result.compressedSize, result.compressedSize / 1024);
                    LOG.info("     Smanjenje:    {:.1f}%", reductionPercent);
                    LOG.info("     Putanja:      {}", result.compressedPath);
                } else {
                    LOG.warn("  ⚠️  Kompresija nije uspela - rezultat je null");
                    failureCount++;
                }

            } catch (Exception e) {
                failureCount++;
                LOG.error("  ❌ Greška pri kompresiji thumbnail-a za video ID={}: {}",
                        video.getId(), e.getMessage());
                LOG.debug("  Stack trace:", e);
            }

            LOG.info("-".repeat(80));
        }

        // Završni izveštaj
        LOG.info("=".repeat(80));
        LOG.info("PERIODIČNA KOMPRESIJA THUMBNAILS - ZAVRŠENA");
        LOG.info("=".repeat(80));
        LOG.info("Statistika:");
        LOG.info("  ✅ Uspešno kompresovano:  {} thumbnail-a", successCount);
        LOG.info("  ⏭️  Preskočeno:            {} thumbnail-a (već kompresovani)", skippedCount);
        LOG.info("  ❌ Neuspešno:              {} thumbnail-a", failureCount);
        LOG.info("  📊 Ukupno video-a:         {} video-a", oldVideos.size());

        if (successCount > 0) {
            double totalReduction = 100.0 * (1.0 - (double) totalCompressedSize / totalOriginalSize);
            LOG.info("Ušteda prostora:");
            LOG.info("  Original veličina:     {} bytes ({} MB)", totalOriginalSize, totalOriginalSize / (1024 * 1024));
            LOG.info("  Kompresovana veličina: {} bytes ({} MB)", totalCompressedSize, totalCompressedSize / (1024 * 1024));
            LOG.info("  Ukupno smanjenje:      {:.1f}%", totalReduction);
            LOG.info("  Ušteda:                {} bytes ({} MB)",
                    (totalOriginalSize - totalCompressedSize),
                    (totalOriginalSize - totalCompressedSize) / (1024 * 1024));
        }

        LOG.info("=".repeat(80));

        return successCount;
    }

    /**
     * Kompresuje thumbnail jednog videa
     */
    private CompressionResult compressThumbnail(Video video) {
        String originalPath = video.getThumbnailPath();

        if (originalPath == null || originalPath.isEmpty()) {
            LOG.warn("    Video ID={} nema postavljenu putanju za thumbnail", video.getId());
            return null;
        }

        File originalFile = new File(originalPath);

        if (!originalFile.exists()) {
            LOG.warn("    Thumbnail fajl ne postoji na putanji: {}", originalPath);
            return null;
        }

        if (!originalFile.canRead()) {
            LOG.error("    Nemam dozvolu za čitanje fajla: {}", originalPath);
            return null;
        }

        try {
            // Kreiraj putanju za kompresovanu sliku
            // Primer: storage/thumbs/abc123.jpg -> storage/thumbs/compressed_abc123.jpg
            Path originalFilePath = Paths.get(originalPath);
            String originalFileName = originalFilePath.getFileName().toString();
            String compressedFileName = compressionPrefix + originalFileName;
            Path compressedFilePath = originalFilePath.getParent().resolve(compressedFileName);

            // Proveri da li parent direktorijum postoji
            Path parentDir = compressedFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                LOG.info("    Kreiran direktorijum: {}", parentDir);
            }

            // KOMPRESIJA koristeći Thumbnailator biblioteku
            // - scale(1.0) = zadržavamo originalne dimenzije (ne menjamo veličinu)
            // - outputQuality() = smanjujemo kvalitet (kompresujemo)
            Thumbnails.of(originalFile)
                    .scale(1.0)                          // Zadržavamo originalne dimenzije
                    .outputQuality(compressionQuality)   // Kompresujemo kvalitet
                    .toFile(compressedFilePath.toFile());

            // Statistika
            long originalSize = Files.size(originalFilePath);
            long compressedSize = Files.size(compressedFilePath);

            return new CompressionResult(
                    compressedFilePath.toString(),
                    originalSize,
                    compressedSize
            );

        } catch (IOException e) {
            LOG.error("    IOException pri kompresiji thumbnail-a: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.error("    Neočekivana greška pri kompresiji thumbnail-a: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Helper klasa za vraćanje rezultata kompresije
     */
    private static class CompressionResult {
        final String compressedPath;
        final long originalSize;
        final long compressedSize;

        CompressionResult(String compressedPath, long originalSize, long compressedSize) {
            this.compressedPath = compressedPath;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }
    }
}