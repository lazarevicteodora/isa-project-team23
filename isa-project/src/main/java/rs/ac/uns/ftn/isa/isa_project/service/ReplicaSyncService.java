package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
    private List<String> replicaUrls;

    /**
     * PUSH sync - šalje update drugim replikama.
     */
    @Async
    public void pushUpdateToOtherReplicas(Long videoId) {
        LOG.info("[{}] Pushing update for video {} to other replicas", replicaId, videoId);

        try {
            // Čitaj SAMO iz tabele trenutne replike
            Map<String, Long> currentCounts = getCurrentLocalCounts(videoId);

            CRDTSyncRequest syncRequest = new CRDTSyncRequest(videoId, replicaId, currentCounts);

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
     * PULL sync - traži stanje od drugih replika i merge-uje.
     */
    @Transactional
    public void pullAndMergeFromOtherReplicas(Long videoId) {
        LOG.debug("[{}] Pulling updates for video {} from other replicas", replicaId, videoId);

        try {
            GCounter localCounter = getLocalGCounter(videoId);

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

            // Primeni merged stanje SAMO na lokalnu tabelu
            applyMergedCountsToLocalTable(videoId, localCounter);

            LOG.debug("[{}] Successfully pulled and merged updates for video {} from {}/{} replicas",
                    replicaId, videoId, successCount, replicaUrls.size());

        } catch (Exception e) {
            LOG.error("[{}] Failed to pull updates for video {}: {}", replicaId, videoId, e.getMessage());
        }
    }

    /**
     * Vraća trenutno stanje SAMO LOKALNE tabele.
     */
    public Map<String, Long> getCurrentLocalCounts(Long videoId) {
        Map<String, Long> counts = new HashMap<>();

        // Čitaj SAMO iz tabele trenutne replike
        viewCountRepository
                .findByVideoIdAndReplicaId(videoId, replicaId)
                .ifPresent(vc -> counts.put(replicaId, vc.getCount()));

        return counts;
    }

    /**
     * Kreira G-Counter iz lokalne tabele.
     */
    private GCounter getLocalGCounter(Long videoId) {
        Map<String, Long> counts = getCurrentLocalCounts(videoId);
        return new GCounter(counts);
    }

    /**
     * Šalje HTTP POST zahtev drugoj replici.
     */
    private void sendSyncRequest(String replicaUrl, CRDTSyncRequest syncRequest) {
        String url = replicaUrl + "/api/crdt/sync";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CRDTSyncRequest> entity = new HttpEntity<>(syncRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send sync request: " + response.getStatusCode());
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
                return new GCounter();
            }

        } catch (Exception e) {
            LOG.error("[{}] Error fetching state from {}: {}", replicaId, replicaUrl, e.getMessage());
            return new GCounter();
        }
    }

    /**
     * Primenjuje merged counts SAMO na lokalnu tabelu trenutne replike.
     *
     * KLJUČNA RAZLIKA: Ne diramo tabele drugih replika!
     * Svaka replika upravlja samo svojom tabelom.
     */
    @Transactional
    public void applyMergedCountsToLocalTable(Long videoId, GCounter mergedCounter) {
        LOG.debug("[{}] Applying merged counts for video {} to local table", replicaId, videoId);

        // Uzmi merged vrednost za TRENUTNU repliku
        Long mergedCount = mergedCounter.getReplicaCount(replicaId);

        // Update SAMO lokalnu tabelu
        ViewCount localViewCount = viewCountRepository
                .findByVideoIdAndReplicaId(videoId, replicaId)
                .orElse(new ViewCount(videoId, replicaId));

        // MAX operacija (CRDT svojstvo)
        if (mergedCount > localViewCount.getCount()) {
            localViewCount.setCount(mergedCount);
            viewCountRepository.save(localViewCount);

            LOG.debug("[{}] Updated local count for video {} to {}",
                    replicaId, videoId, mergedCount);
        }
    }

    /**
     * Prima sync request od druge replike.
     */
    @Transactional
    public void receiveSyncRequest(CRDTSyncRequest syncRequest) {
        LOG.info("[{}] Received sync request from {} for video {}",
                replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        try {
            GCounter localCounter = getLocalGCounter(syncRequest.getVideoId());
            GCounter remoteCounter = new GCounter(syncRequest.getCounts());

            GCounter merged = localCounter.merge(remoteCounter);

            applyMergedCountsToLocalTable(syncRequest.getVideoId(), merged);

            LOG.info("[{}] Successfully merged sync request from {} for video {}",
                    replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        } catch (Exception e) {
            LOG.error("[{}] Failed to merge sync request from {}: {}",
                    replicaId, syncRequest.getSourceReplicaId(), e.getMessage());
        }
    }
}