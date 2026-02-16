package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.PopularVideoResult;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.model.ViewLog;
import rs.ac.uns.ftn.isa.isa_project.repository.PopularVideoResultRepository;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ETLPipelineService {

    private static final Logger LOG = LoggerFactory.getLogger(ETLPipelineService.class);

    @Autowired
    private ViewLogRepository viewLogRepository;

    @Autowired
    private PopularVideoResultRepository popularVideoResultRepository;

    /**
     * Pokreće ETL pipeline.
     * Extract -> Transform -> Load
     */
    @Transactional
    public void runPipeline() {
        LOG.info("=== ETL Pipeline started at {} ===", LocalDateTime.now());
        LocalDateTime runAt = LocalDateTime.now();

        try {
            // EXTRACT
            List<ViewLog> rawData = extract();
            LOG.info("[ETL] Extracted {} view log entries", rawData.size());

            // TRANSFORM
            List<VideoScore> scores = transform(rawData);
            LOG.info("[ETL] Transformed into {} video scores", scores.size());

            // LOAD
            load(scores, runAt);
            LOG.info("[ETL] Pipeline completed successfully at {}", LocalDateTime.now());

        } catch (Exception e) {
            LOG.error("[ETL] Pipeline failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * EXTRACT: Učitava view logove za poslednjih 7 dana.
     */
    private List<ViewLog> extract() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<ViewLog> logs = viewLogRepository.findAllSince(sevenDaysAgo);
        LOG.info("[ETL][EXTRACT] Found {} view logs since {}", logs.size(), sevenDaysAgo);
        return logs;
    }

    /**
     * TRANSFORM: Računa popularity score za svaki video.
     *
     * Weight formula:
     * - pregledi od pre x dana * težina (7 - x + 1)
     * - pregledi od pre 1 dana (juče) * 7
     * - pregledi od pre 7 dana * 1
     */
    private List<VideoScore> transform(List<ViewLog> viewLogs) {
        LocalDate today = LocalDate.now();

        // Grupiši logove po videu
        Map<Video, List<ViewLog>> byVideo = viewLogs.stream()
                .collect(Collectors.groupingBy(ViewLog::getVideo));

        List<VideoScore> scores = new ArrayList<>();

        for (Map.Entry<Video, List<ViewLog>> entry : byVideo.entrySet()) {
            Video video = entry.getKey();
            List<ViewLog> logs = entry.getValue();

            double score = 0.0;

            for (ViewLog log : logs) {
                // Koliko dana pre danas je bio ovaj pregled
                long daysAgo = today.toEpochDay() - log.getViewDate().toEpochDay();

                // daysAgo = 0 znači danas, daysAgo = 1 znači juče, itd.
                // Težina: 7 - daysAgo + 1, ali daysAgo mora biti 1..7
                // Dakle za daysAgo = 1 (juče): weight = 7, za daysAgo = 7: weight = 1
                if (daysAgo >= 1 && daysAgo <= 7) {
                    double weight = 7 - daysAgo + 1;
                    score += log.getViewCount() * weight;
                } else if (daysAgo == 0) {
                    // Pregledi danas - tretiramo kao juče (weight = 7)
                    score += log.getViewCount() * 7;
                }
            }

            scores.add(new VideoScore(video, score));
            LOG.debug("[ETL][TRANSFORM] Video '{}' (id={}) score: {}", video.getTitle(), video.getId(), score);
        }

        // Sortiraj opadajuće po scoru
        scores.sort(Comparator.comparingDouble(VideoScore::score).reversed());

        return scores;
    }

    /**
     * LOAD: Upisuje top 3 videa u bazu.
     */
    private void load(List<VideoScore> scores, LocalDateTime runAt) {
        List<PopularVideoResult> results = new ArrayList<>();

        int limit = Math.min(3, scores.size());

        for (int i = 0; i < limit; i++) {
            VideoScore vs = scores.get(i);
            PopularVideoResult result = new PopularVideoResult(
                    runAt,
                    i + 1,          // rank: 1, 2, 3
                    vs.video(),
                    vs.score()
            );
            results.add(result);
            LOG.info("[ETL][LOAD] Rank {}: video '{}' (id={}) with score {}",
                    i + 1, vs.video().getTitle(), vs.video().getId(), vs.score());
        }

        popularVideoResultRepository.saveAll(results);
        LOG.info("[ETL][LOAD] Saved {} popular video results", results.size());
    }

    /**
     * Vraća top 3 iz poslednjeg pokretanja pipeline-a.
     */
    public List<PopularVideoResult> getLatestPopularVideos() {
        return popularVideoResultRepository.findLatestResults();
    }

    // Interni record za međufazu transform -> load
    private record VideoScore(Video video, double score) {}
}