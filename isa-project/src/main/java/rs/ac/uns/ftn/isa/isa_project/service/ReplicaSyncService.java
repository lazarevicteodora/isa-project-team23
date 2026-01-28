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
import org.springframework.web.client.RestTemplate;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;
import rs.ac.uns.ftn.isa.isa_project.dto.CRDTSyncRequest;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica1;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica2;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplica1Repository;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplica2Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servis za komunikaciju između replika.
 *
 * Implementira PUSH i PULL strategije sinhronizacije:
 * 1. PUSH: Nakon svake izmene, šalje update drugim replikama (opciono)
 * 2. PULL: Periodično traži izmene od drugih replika
 * 3. SYNC ON READ: Kada klijent traži podatke, prvo sinhronizuje stanje
 */
@Service
public class ReplicaSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicaSyncService.class);

    @Autowired
    private ViewCountReplica1Repository replica1Repository;

    @Autowired
    private ViewCountReplica2Repository replica2Repository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.replica.urls}")
    private List<String> replicaUrls; // Lista URL-ova drugih replika (npr. http://localhost:8081,http://localhost:8082)

    /**
     * ASINHRONO šalje trenutno stanje G-Counter-a za dati video svim drugim replikama.
     * Ova metoda se poziva NAKON SVAKE MODIFIKACIJE (push-based sync).
     *
     * @param videoId ID videa
     */
    @Async
    public void pushUpdateToOtherReplicas(Long videoId) {
        LOG.info("[{}] Pushing update for video {} to other replicas", replicaId, videoId);

        try {
            // 1. Pročitaj trenutno lokalno stanje
            Map<String, Long> currentCounts = getCurrentLocalCounts(videoId);

            // 2. Kreiraj sync request
            CRDTSyncRequest syncRequest = new CRDTSyncRequest(videoId, replicaId, currentCounts);

            // 3. Pošalji svim drugim replikama
            for (String replicaUrl : replicaUrls) {
                if (!replicaUrl.isEmpty()) {
                    sendSyncRequest(replicaUrl, syncRequest);
                }
            }

            LOG.info("[{}] Successfully pushed update for video {} to {} replicas",
                    replicaId, videoId, replicaUrls.size());

        } catch (Exception e) {
            LOG.error("[{}] Failed to push update for video {}: {}", replicaId, videoId, e.getMessage());
        }
    }

    /**
     * SINHRONO traži stanje od svih drugih replika i vrši MERGE (pull-based sync).
     * Ova metoda se poziva PRE ČITANJA PODATAKA ili PERIODIČNO.
     *
     * @param videoId ID videa
     */
    public void pullAndMergeFromOtherReplicas(Long videoId) {
        LOG.info("[{}] Pulling updates for video {} from other replicas", replicaId, videoId);

        try {
            GCounter localCounter = getLocalGCounter(videoId);

            //Traži stanje od svih drugih replika
            for (String replicaUrl : replicaUrls) {
                if (!replicaUrl.isEmpty()) {
                    try {
                        GCounter remoteCounter = fetchRemoteGCounter(replicaUrl, videoId);
                        localCounter = localCounter.merge(remoteCounter);
                    } catch (Exception e) {
                        LOG.warn("[{}] Failed to fetch from replica {}: {}", replicaId, replicaUrl, e.getMessage());
                    }
                }
            }

            // Primeni merged stanje na lokalnu bazu
            applyMergedCounts(videoId, localCounter);

            LOG.info("[{}] Successfully pulled and merged updates for video {}", replicaId, videoId);

        } catch (Exception e) {
            LOG.error("[{}] Failed to pull updates for video {}: {}", replicaId, videoId, e.getMessage());
        }
    }

    /**
     * Vraća trenutno lokalno stanje kao Map<replicaId, count>.
     * Javna metoda koju koristi kontroler.
     */
    public Map<String, Long> getCurrentLocalCounts(Long videoId) {
        Map<String, Long> counts = new HashMap<>();

        // Pročitaj iz obe tabele
        Long count1 = replica1Repository.findByVideoId(videoId)
                .map(ViewCountReplica1::getCount)
                .orElse(0L);

        Long count2 = replica2Repository.findByVideoId(videoId)
                .map(ViewCountReplica2::getCount)
                .orElse(0L);

        counts.put("replica-1", count1);
        counts.put("replica-2", count2);

        return counts;
    }

    /**
     * Vraća lokalni G-Counter za dati video.
     */
    private GCounter getLocalGCounter(Long videoId) {
        Map<String, Long> counts = getCurrentLocalCounts(videoId);
        return new GCounter(counts);
    }

    /**
     * Šalje HTTP POST zahtev na drugu repliku sa sync request-om.
     */
    private void sendSyncRequest(String replicaUrl, CRDTSyncRequest syncRequest) {
        try {
            String url = replicaUrl + "/api/crdt/sync";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CRDTSyncRequest> entity = new HttpEntity<>(syncRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("[{}] Successfully sent sync request to {}", replicaId, replicaUrl);
            } else {
                LOG.warn("[{}] Failed to send sync request to {}: {}", replicaId, replicaUrl, response.getStatusCode());
            }

        } catch (Exception e) {
            LOG.error("[{}] Error sending sync request to {}: {}", replicaId, replicaUrl, e.getMessage());
        }
    }

    /**
     * Traži G-Counter stanje od udaljene replike.
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
     * Koristi MAX vrednost između lokalnog i merged stanja (CRDT svojstvo).
     */
    private void applyMergedCounts(Long videoId, GCounter mergedCounter) {
        // Update replica-1 tabelu
        Long mergedCount1 = mergedCounter.getReplicaCount("replica-1");
        ViewCountReplica1 viewCount1 = replica1Repository.findByVideoId(videoId)
                .orElse(new ViewCountReplica1(videoId));

        if (mergedCount1 > viewCount1.getCount()) {
            viewCount1.setCount(mergedCount1);
            replica1Repository.save(viewCount1);
            LOG.debug("[{}] Updated replica-1 count for video {} to {}", replicaId, videoId, mergedCount1);
        }

        // Update replica-2 tabelu
        Long mergedCount2 = mergedCounter.getReplicaCount("replica-2");
        ViewCountReplica2 viewCount2 = replica2Repository.findByVideoId(videoId)
                .orElse(new ViewCountReplica2(videoId));

        if (mergedCount2 > viewCount2.getCount()) {
            viewCount2.setCount(mergedCount2);
            replica2Repository.save(viewCount2);
            LOG.debug("[{}] Updated replica-2 count for video {} to {}", replicaId, videoId, mergedCount2);
        }
    }

    /**
     * Prima sync request od druge replike i vrši MERGE.
     * Ova metoda se poziva kada druga replika pošalje update.
     *
     * @param syncRequest Sync request sa stanjem G-Counter-a
     */
    public void receiveSyncRequest(CRDTSyncRequest syncRequest) {
        LOG.info("[{}] Received sync request from {} for video {}",
                replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        try {
            GCounter localCounter = getLocalGCounter(syncRequest.getVideoId());
            GCounter remoteCounter = new GCounter(syncRequest.getCounts());

            GCounter merged = localCounter.merge(remoteCounter);

            applyMergedCounts(syncRequest.getVideoId(), merged);

            LOG.info("[{}] Successfully merged sync request from {} for video {}",
                    replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        } catch (Exception e) {
            LOG.error("[{}] Failed to merge sync request from {}: {}",
                    replicaId, syncRequest.getSourceReplicaId(), e.getMessage());
        }
    }
}