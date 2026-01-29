package rs.ac.uns.ftn.isa.isa_project.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.VideoRepository;
import rs.ac.uns.ftn.isa.isa_project.service.ReplicaSyncService;

import java.util.List;

/**
 * Scheduled task za periodičnu sinhronizaciju view count-ova između replika.
 *
 * Strategija: Svake 30 sekundi, prolazi kroz sve video-e i vrši pull sync
 * sa drugim replikama. Ovo osigurava eventual consistency čak i ako push sync
 * ne radi ili ako postoje mrežni problemi.
 */
@Component
public class CRDTSyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CRDTSyncScheduler.class);

    @Autowired
    private ReplicaSyncService syncService;

    @Autowired
    private VideoRepository videoRepository;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.sync.periodic-enabled:true}") // Default: periodična sync je omogućena
    private boolean periodicSyncEnabled;

    /**
     * Periodična sinhronizacija - izvršava se svakih 30 sekundi.
     *
     * fixedRate = 30000ms (30 sekundi)
     * initialDelay = 10000ms (čeka 10s nakon startup-a pre prvog izvršavanja)
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void periodicSync() {
        if (!periodicSyncEnabled) {
            return;
        }

        LOG.info("[{}] Starting periodic CRDT sync...", replicaId);

        try {
            // Uzmi sve video-e iz baze
            List<Video> videos = videoRepository.findAll();

            int successCount = 0;
            int failureCount = 0;

            // Za svaki video, uradi pull sync
            for (Video video : videos) {
                try {
                    syncService.pullAndMergeFromOtherReplicas(video.getId());
                    successCount++;
                } catch (Exception e) {
                    LOG.error("[{}] Failed to sync video {}: {}", replicaId, video.getId(), e.getMessage());
                    failureCount++;
                }
            }

            LOG.info("[{}] Periodic sync completed: {} successful, {} failed out of {} videos",
                    replicaId, successCount, failureCount, videos.size());

        } catch (Exception e) {
            LOG.error("[{}] Fatal error during periodic sync: {}", replicaId, e.getMessage());
        }
    }

    /**
     * OPCIONO: Manje frekventna duboka sinhronizacija - svakih 5 minuta.
     * Ova metoda može da uključi dodatne provere konzistentnosti.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000) // 5 minuta, čeka 1 minut na početku
    public void deepSync() {
        if (!periodicSyncEnabled) {
            return;
        }

        LOG.info("[{}] Starting deep CRDT sync (heavy operation)...", replicaId);

        // Ovde možeš dodati dodatnu logiku za duboku sinhronizaciju
        // Npr. proveru konzistentnosti, garbage collection starih view count-ova, itd.

        LOG.info("[{}] Deep sync completed", replicaId);
    }
}