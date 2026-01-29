package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rs.ac.uns.ftn.isa.isa_project.dto.CRDTSyncRequest;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplicaRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servis za sinhronizaciju CRDT stanja između replika.
 */
@Service
public class ReplicaSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicaSyncService.class);

    @Autowired
    private ViewCountReplicaRepository replicaDAO;

    @Autowired
    private ReplicaService replicaRegistry;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.replica.urls:}")
    private String replicaUrlsConfig;

    /**
     * Push-based sinhronizacija: Šalje update SVIM drugim replikama.
     */
    public void pushUpdateToOtherReplicas(Long videoId) {
        List<String> otherReplicaUrls = getOtherReplicaUrls();

        if (otherReplicaUrls.isEmpty()) {
            LOG.debug("[{}] No other replicas configured for push sync", replicaId);
            return;
        }

        Map<String, Long> counts = getCurrentLocalCounts(videoId);
        CRDTSyncRequest syncRequest = new CRDTSyncRequest(videoId, replicaId, counts);

        for (String replicaUrl : otherReplicaUrls) {
            try {
                String syncEndpoint = replicaUrl + "/api/crdt/sync";
                restTemplate.postForEntity(syncEndpoint, syncRequest, String.class);
                LOG.debug("[{}] Pushed update to {}", replicaId, replicaUrl);
            } catch (Exception e) {
                LOG.warn("[{}] Failed to push update to {}: {}",
                        replicaId, replicaUrl, e.getMessage());
            }
        }
    }

    /**
     * Pull-based sinhronizacija: Povlači stanje SA SVIH drugih replika i merguje.
     */
    @Transactional
    public void pullAndMergeFromOtherReplicas(Long videoId) {
        List<String> otherReplicaUrls = getOtherReplicaUrls();

        if (otherReplicaUrls.isEmpty()) {
            LOG.debug("[{}] No other replicas configured for pull sync", replicaId);
            return;
        }

        List<CRDTSyncRequest> remoteStates = new ArrayList<>();

        for (String replicaUrl : otherReplicaUrls) {
            try {
                String stateEndpoint = replicaUrl + "/api/crdt/state/" + videoId;
                CRDTSyncRequest remoteState = restTemplate.getForObject(
                        stateEndpoint,
                        CRDTSyncRequest.class
                );

                if (remoteState != null) {
                    remoteStates.add(remoteState);
                    LOG.debug("[{}] Pulled state from {}: {}",
                            replicaId, replicaUrl, remoteState.getCounts());
                }
            } catch (Exception e) {
                LOG.warn("[{}] Failed to pull state from {}: {}",
                        replicaId, replicaUrl, e.getMessage());
            }
        }

        if (!remoteStates.isEmpty()) {
            mergeRemoteStates(videoId, remoteStates);
        }
    }

    /**
     * Prima sync request od druge replike (push-based).
     */
    @Transactional
    public void receiveSyncRequest(CRDTSyncRequest syncRequest) {
        LOG.info("[{}] Received sync request from {} for video {}",
                replicaId, syncRequest.getSourceReplicaId(), syncRequest.getVideoId());

        mergeRemoteStates(syncRequest.getVideoId(), Collections.singletonList(syncRequest));
    }

    /**
     * CRDT merge operacija.
     */
    private void mergeRemoteStates(Long videoId, List<CRDTSyncRequest> remoteStates) {
        List<String> allReplicaIds = replicaRegistry.getAllReplicaIds();
        Map<String, Long> localCounts = new HashMap<>();

        for (String repId : allReplicaIds) {
            replicaDAO.findByVideoId(videoId, repId)
                    .ifPresent(vc -> localCounts.put(repId, vc.getCount()));
        }

        Map<String, Long> mergedCounts = new HashMap<>(localCounts);

        for (CRDTSyncRequest remoteState : remoteStates) {
            for (Map.Entry<String, Long> entry : remoteState.getCounts().entrySet()) {
                String remoteReplicaId = entry.getKey();
                Long remoteCount = entry.getValue();

                mergedCounts.merge(remoteReplicaId, remoteCount, Math::max);
            }
        }

        for (Map.Entry<String, Long> entry : mergedCounts.entrySet()) {
            String repId = entry.getKey();
            Long newCount = entry.getValue();

            replicaRegistry.ensureTableExists(repId);

            Optional<ViewCountReplica> existing = replicaDAO.findByVideoId(videoId, repId);

            if (existing.isPresent()) {
                Long currentCount = existing.get().getCount();
                if (newCount > currentCount) {
                    LOG.debug("[{}] Updating count for replica {} from {} to {}",
                            replicaId, repId, currentCount, newCount);
                    replicaDAO.update(videoId, newCount, repId);
                }
            } else {
                LOG.debug("[{}] Creating new entry for replica {} with count {}",
                        replicaId, repId, newCount);
                replicaDAO.create(videoId, repId);
                replicaDAO.update(videoId, newCount, repId);
            }
        }

        LOG.info("[{}] Merge completed for video {}, final state: {}",
                replicaId, videoId, mergedCounts);
    }

    /**
     * Vraća trenutno lokalno stanje ZA SVE REPLIKE.
     */
    public Map<String, Long> getCurrentLocalCounts(Long videoId) {
        List<String> allReplicaIds = replicaRegistry.getAllReplicaIds();
        Map<String, Long> counts = new HashMap<>();

        for (String repId : allReplicaIds) {
            replicaDAO.findByVideoId(videoId, repId)
                    .ifPresent(vc -> counts.put(repId, vc.getCount()));
        }

        return counts;
    }

    private List<String> getOtherReplicaUrls() {
        if (replicaUrlsConfig == null || replicaUrlsConfig.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(replicaUrlsConfig.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());
    }
}