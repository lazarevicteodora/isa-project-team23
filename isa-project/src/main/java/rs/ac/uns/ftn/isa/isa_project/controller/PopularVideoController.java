package rs.ac.uns.ftn.isa.isa_project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.dto.PopularVideoDTO;
import rs.ac.uns.ftn.isa.isa_project.model.PopularVideoResult;
import rs.ac.uns.ftn.isa.isa_project.service.ETLPipelineService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/popular-videos")
@CrossOrigin(origins = "http://localhost:4200")
public class PopularVideoController {

    private static final Logger LOG = LoggerFactory.getLogger(PopularVideoController.class);

    @Autowired
    private ETLPipelineService etlPipelineService;

    /**
     * GET /api/popular-videos
     * Vraća top 3 najpopularnija videa iz poslednjeg ETL pokretanja.
     * Dostupno samo ulogovanim korisnicima.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PopularVideoDTO>> getPopularVideos() {
        List<PopularVideoResult> results = etlPipelineService.getLatestPopularVideos();

        if (results.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<PopularVideoDTO> dtos = results.stream()
                .map(PopularVideoDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/popular-videos/run
     * Manuelno pokretanje ETL pipeline-a (za testiranje/debugging).
     * Samo za ADMIN korisnike.
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> triggerPipeline() {
        LOG.info("Manual ETL pipeline trigger requested");
        try {
            etlPipelineService.runPipeline();
            return ResponseEntity.ok("ETL pipeline executed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("ETL pipeline failed: " + e.getMessage());
        }
    }
}