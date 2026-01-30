package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;
import rs.ac.uns.ftn.isa.isa_project.dto.CRDTSyncRequest;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCount;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementira PUSH i PULL strategije sinhronizacije:
 * 1. PUSH: Nakon svake izmene, salje update drugim replikama (opciono)
 * 2. PULL: Periodicno trazi izmene od drugih replika
 * 3. SYNC ON READ: Kada klijent trazi podatke, prvo sinhronizuje stanje
 */
@Service
public class ReplicaSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicaSyncService.class);

    @Autowired
    private ViewCountRepository viewCountRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.replica.urls}")
    private List<String> replicaUrls; // Lista URL-ova drugih replika

    /**
     * ASINHRONO Salje trenutno stanje G-Counter-a za dati video svim drugim replikama.
     * Ova metoda se poziva NAKON SVAKE MODIFIKACIJE (push-based sync).
     *
     * @param videoId ID videa
     */
    @Async
    public void pushUpdateToOtherReplicas(Long videoId) {
        LOG.info("[{}] Pushing update for video {} to other replicas", replicaId, videoId);

        try {
            // 1. Procitaj trenutno lokalno stanje
            Map<String, Long> currentCounts = getCurrentLocalCounts(videoId);

            // 2. Kreiraj sync request
            CRDTSyncRequest syncRequest = new CRDTSyncRequest(videoId, replicaId, currentCounts);

            // 3. Posalji svim drugim replikama
            int successCount = 0;
            for (String replicaUrl : replicaUrls) {
                if (!replicaUrl.isEmpty()) {
                    try {
                        sendSyncRequest(replicaUrl, syncRequest);
                        successCount++;
                    } catch (Exception e) {
                        LOG.warn("[{}] Failed to push to {}: {}", replicaId, replicaUrl, e.getMessage());
                    }
                }
            }

            LOG.info("[{}] Successfully pushed update for video {} to {}/{} replicas",
                    replicaId, videoId, successCount, replicaUrls.size());

        } catch (Exception e) {
            LOG.error("[{}] Failed to push update for video {}: {}", replicaId, videoId, e.getMessage());
        }
    }

    /**
     * SINHRONO trazi stanje od svih drugih replika i vrsi MERGE (pull-based sync).
     * Ova metoda se poziva PRE CITANJA PODATAKA ili PERIODICNO.
     *
     * @param videoId ID videa
     */
    @Transactional
    public void pullAndMergeFromOtherReplicas(Long videoId) {
        LOG.debug("[{}] Pulling updates for video {} from other replicas", replicaId, videoId);

        try {
            // 1. Ucitaj lokalni G-Counter
            GCounter localCounter = getLocalGCounter(videoId);

            // 2. Trazi stanje od svih drugih replika i merge-uj
            int successCount = 0;
            for (String replicaUrl : replicaUrls) {
                if (!replicaUrl.isEmpty()) {
                    try {
                        GCounter remoteCounter = fetchRemoteGCounter(replicaUrl, videoId);
                        localCounter = localCounter.merge(remoteCounter);
                        successCount++;
                    } catch (Exception e) {
                        LOG.warn("[{}] Failed to fetch from replica {}: {}", replicaId, replicaUrl, e.getMessage());
                    }
                }
            }

            // 3. Primeni merged stanje na lokalnu bazu
            applyMergedCounts(videoId, localCounter);

            LOG.debug("[{}] Successfully pulled and merged updates for video {} from {}/{} replicas",
                    replicaId, videoId, successCount, replicaUrls.size());

        } catch (Exception e) {
            LOG.error("[{}] Failed to pull updates for video {}: {}", replicaId, videoId, e.getMessage());
        }
    }

    /**
     * Vraca trenutno lokalno stanje kao Map<replicaId, count>.
     * Javna metoda koju koristi kontroler.
     */
    public Map<String, Long> getCurrentLocalCounts(Long videoId) {
        Map<String, Long> counts = new HashMap<>();

        // Ucitaj SVE replike za ovaj video
        List<ViewCount> allReplicas = viewCountRepository.findAllByVideoId(videoId);

        // Popuni mapu
        for (ViewCount vc : allReplicas) {
            counts.put(vc.getReplicaId(), vc.getCount());
        }

        LOG.trace("[{}] Current local counts for video {}: {}", replicaId, videoId, counts);

        return counts;
    }

    /**
     * Vraca lokalni G-Counter za dati video.
     * Dinamički - radi sa bilo kojim brojem replika.
     */
    private GCounter getLocalGCounter(Long videoId) {
        Map<String, Long> counts = getCurrentLocalCounts(videoId);
        return new GCounter(counts);
    }

    /**
     * Salje HTTP POST zahtev na drugu repliku sa sync request-om.
     */
    private void sendSyncRequest(String replicaUrl, CRDTSyncRequest syncRequest) {
        try {
            String url = replicaUrl + "/api/crdt/sync";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CRDTSyncRequest> entity = new HttpEntity<>(syncRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.trace("[{}] Successfully sent sync request to {}", replicaId, replicaUrl);
            } else {
                LOG.warn("[{}] Failed to send sync request to {}: {}", replicaId, replicaUrl, response.getStatusCode());
            }

        } catch (Exception e) {
            LOG.error("[{}] Error sending sync request to {}: {}", replicaId, replicaUrl, e.getMessage());
            throw e; // Rethrow za praćenje grešaka
        }
    }

    /**
     * Trazi G-Counter stanje od udaljene replike.
     */
    private GCounter fetchRemoteGCounter(String replicaUrl, Long videoId) {
        try {
            String url = replicaUrl + "/api/crdt/state/" + videoId;

            ResponseEntity<CRDTSyncRequest> response = restTemplate.getForEntity(url, CRDTSyncRequest.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                CRDTSyncRequest remoteState = response.getBody();
                return new GCounter(remoteState.getCounts());
            } else {
                LOG.warn("[{}] Failed to fetch state from {}: {}", replicaId, replicaUrl, response.getStatusCode());
                return new GCounter(); // Prazan counter
            }

        } catch (Exception e) {
            LOG.error("[{}] Error fetching state from {}: {}", replicaId, replicaUrl, e.getMessage());
            return new GCounter(); // Prazan counter
        }
    }

    /**
     * Primenjuje merged G-Counter stanje na lokalnu bazu.
     * Koristi MAX vrednost izmedju lokalnog i merged stanja (CRDT svojstvo).
     * Iterira kroz SVE replike u merged counter-u!
     */
    @Transactional
    public void applyMergedCounts(Long videoId, GCounter mergedCounter) {
        LOG.debug("[{}] Applying merged counts for video {}", replicaId, videoId);

        // Iteracija kroz SVE replike u merged counter-u
        Map<String, Long> mergedCounts = mergedCounter.getCounts();

        int updatedCount = 0;
        for (Map.Entry<String, Long> entry : mergedCounts.entrySet()) {
            String currentReplicaId = entry.getKey();
            Long mergedCount = entry.getValue();

            // Pronadji ili kreiraj zapis za ovu repliku
            ViewCount viewCount = viewCountRepository
                    .findByVideoIdAndReplicaId(videoId, currentReplicaId)
                    .orElse(new ViewCount(videoId, currentReplicaId));

            // Update SAMO ako je merged vrednost veća (CRDT MAX operacija)
            if (mergedCount > viewCount.getCount()) {
                viewCount.setCount(mergedCount);
                viewCountRepository.save(viewCount);
                updatedCount++;

                LOG.trace("[{}] Updated {} count for video {} to {}",
                        replicaId, currentReplicaId, videoId, mergedCount);
            }
        }

        LOG.debug("[{}] Applied merged counts for video {}: updated {} replica entries",
                replicaId, videoId, updatedCount);
    }

    /**
     * Prima sync request od druge replike i vrsi MERGE.
     * Ova metoda se poziva kada druga replika posalje update.
     *
     * @param syncRequest Sync request sa stanjem G-Counter-a
     */
    @Transactional
    public void receiveSyncRequest(CRDTSyncRequest syncRequest) {
        LOG.info("[{}] Received sync request from {} for video {}",
                replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        try {
            // 1. Ucitaj lokalni counter
            GCounter localCounter = getLocalGCounter(syncRequest.getVideoId());

            // 2. Kreiraj remote counter iz sync request-a
            GCounter remoteCounter = new GCounter(syncRequest.getCounts());

            // 3. MERGE
            GCounter merged = localCounter.merge(remoteCounter);

            // 4. Primeni merged vrednosti
            applyMergedCounts(syncRequest.getVideoId(), merged);

            LOG.info("[{}] Successfully merged sync request from {} for video {}",
                    replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        } catch (Exception e) {
            LOG.error("[{}] Failed to merge sync request from {}: {}",
                    replicaId, syncRequest.getSourceReplicaId(), e.getMessage());
            throw e;
        }
    }
}