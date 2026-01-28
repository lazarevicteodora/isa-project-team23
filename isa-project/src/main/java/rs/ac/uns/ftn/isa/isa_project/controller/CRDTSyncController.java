package rs.ac.uns.ftn.isa.isa_project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.dto.CRDTSyncRequest;
import rs.ac.uns.ftn.isa.isa_project.service.ReplicaSyncService;

import java.util.Map;

/**
 * Kontroler za komunikaciju između replika.
 *
 * Endpointi:
 * - POST /api/crdt/sync - Prima sync request od druge replike
 * - GET /api/crdt/state/{videoId} - Vraća trenutno stanje G-Counter-a
 * - POST /api/crdt/sync/pull/{videoId} - Ručno pokreće pull sinhronizaciju
 */
@RestController
@RequestMapping("/api/crdt")
@CrossOrigin(origins = "*") // Dozvoli cross-origin komunikaciju između replika
public class CRDTSyncController {

    private static final Logger LOG = LoggerFactory.getLogger(CRDTSyncController.class);

    @Autowired
    private ReplicaSyncService syncService;

    @Value("${crdt.replica.id}")
    private String replicaId;

    /**
     * Endpoint koji prima sync request od druge replike.
     * Poziva se automatski kada druga replika šalje update (push-based sync).
     *
     * POST /api/crdt/sync
     * Body: { "videoId": 1, "sourceReplicaId": "replica-1", "counts": {"replica-1": 10, "replica-2": 5} }
     */
    @PostMapping("/sync")
    public ResponseEntity<String> receiveSyncRequest(@RequestBody CRDTSyncRequest syncRequest) {
        LOG.info("[{}] Received sync request from {} for video {}",
                replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        try {
            syncService.receiveSyncRequest(syncRequest);
            return ResponseEntity.ok("Sync request processed successfully");
        } catch (Exception e) {
            LOG.error("[{}] Failed to process sync request: {}", replicaId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to process sync request: " + e.getMessage());
        }
    }

    /**
     * Endpoint koji vraća trenutno stanje G-Counter-a za dati video.
     * Poziva se od strane drugih replika kada traže stanje (pull-based sync).
     *
     * GET /api/crdt/state/{videoId}
     * Response: { "videoId": 1, "sourceReplicaId": "replica-1", "counts": {"replica-1": 10, "replica-2": 5} }
     */
    @GetMapping("/state/{videoId}")
    public ResponseEntity<CRDTSyncRequest> getReplicaState(@PathVariable Long videoId) {
        LOG.info("[{}] Received state request for video {}", replicaId, videoId);

        try {
            Map<String, Long> counts = syncService.getCurrentLocalCounts(videoId);
            CRDTSyncRequest response = new CRDTSyncRequest(videoId, replicaId, counts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("[{}] Failed to get replica state: {}", replicaId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint za ručno pokretanje pull sinhronizacije.
     * Koristan za testiranje i debugging.
     *
     * POST /api/crdt/sync/pull/{videoId}
     */
    @PostMapping("/sync/pull/{videoId}")
    public ResponseEntity<String> manualPullSync(@PathVariable Long videoId) {
        LOG.info("[{}] Manual pull sync triggered for video {}", replicaId, videoId);

        try {
            syncService.pullAndMergeFromOtherReplicas(videoId);
            return ResponseEntity.ok("Pull sync completed successfully for video " + videoId);
        } catch (Exception e) {
            LOG.error("[{}] Failed to perform manual pull sync: {}", replicaId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to perform pull sync: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint za proveru da li replika radi.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "replicaId", replicaId
        ));
    }
}