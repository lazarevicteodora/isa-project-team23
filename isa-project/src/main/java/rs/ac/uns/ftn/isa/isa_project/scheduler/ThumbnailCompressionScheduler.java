package rs.ac.uns.ftn.isa.isa_project.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.service.ThumbnailCompressionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ThumbnailCompressionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ThumbnailCompressionScheduler.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Autowired
    private ThumbnailCompressionService thumbnailCompressionService;

    /**
     * Periodična kompresija thumbnail slika.
     *
     * Cron izraz se čita iz application.properties:
     * thumbnail.compression.cron=0 0 0 * * *
     *
     * Default vrednost (ako nije konfigurisan): svaki dan u ponoć
     */
    @Scheduled(cron = "${thumbnail.compression.cron:0 0 0 * * *}")
    public void compressThumbnails() {
        LocalDateTime startTime = LocalDateTime.now();

        LOG.info("");
        LOG.info("═".repeat(78));
        LOG.info(centerText("PERIODIČNA KOMPRESIJA THUMBNAILS - POKRENUTA", 76));
        LOG.info("═".repeat(78));
        LOG.info("Vreme pokretanja: " + padRight(startTime.format(DATE_FORMAT), 57));
        LOG.info("═".repeat(78));
        LOG.info("");

        try {
            int compressedCount = thumbnailCompressionService.compressOldThumbnails();

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            LOG.info("");
            LOG.info("═".repeat(78));
            LOG.info(centerText("PERIODIČNA KOMPRESIJA THUMBNAILS - ZAVRŠENA", 76));
            LOG.info("═".repeat(78));
            LOG.info("Vreme završetka:        " + padRight(endTime.format(DATE_FORMAT), 52));
            LOG.info("Trajanje:               " + padRight(durationSeconds + " sekundi", 52));
            LOG.info("Kompresovano thumbnails: " + padRight(String.valueOf(compressedCount), 51));
            LOG.info("═".repeat(78));

            if (compressedCount > 0) {
                LOG.info("Status: ✅ USPEŠNO - Kompresovano " + padRight(compressedCount + " thumbnail-a", 40));
            } else {
                LOG.info("Status: ℹ️  INFO - Nema thumbnail-a za kompresiju" + padRight("", 26));
            }

            LOG.info("═".repeat(78));
            LOG.info("");

        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            LOG.error("");
            LOG.error("═".repeat(78));
            LOG.error(centerText("PERIODIČNA KOMPRESIJA THUMBNAILS - GREŠKA!", 76));
            LOG.error("╠" + "═".repeat(78));
            LOG.error("Vreme greške: " + padRight(endTime.format(DATE_FORMAT), 63));
            LOG.error("Trajanje:     " + padRight(durationSeconds + " sekundi", 63));
            LOG.error("=".repeat(78));
            LOG.error("Greška: " + padRight(truncate(e.getMessage(), 68), 69));
            LOG.error("═".repeat(78));
            LOG.error("", e);
            LOG.error("");
        }
    }

    /**
     * Centrira tekst u okviru određene širine
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }

        int totalPadding = width - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;

        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }

    /**
     * Dodaje razmake sa desne strane do određene širine
     */
    private String padRight(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }

    /**
     * Skraćuje tekst na određenu dužinu
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}