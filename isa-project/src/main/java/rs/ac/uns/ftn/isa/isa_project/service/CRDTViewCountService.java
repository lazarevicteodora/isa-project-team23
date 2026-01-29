package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplicaRepository;

import java.util.List;

/**
 * Servis za upravljanje view count-ovima koristeći CRDT.
 * Podržava proizvoljan broj replika sa odvojenim tabelama.
 */
@Service
public class CRDTViewCountService {

    private static final Logger LOG = LoggerFactory.getLogger(CRDTViewCountService.class);

    @Autowired
    private ViewCountReplicaRepository replicaDAO;

    @Autowired
    private ReplicaSyncService syncService;

    @Autowired
    private ReplicaService replicaRegistry;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.sync.push-enabled:true}")
    private boolean pushEnabled;

    /**
     * Inkrementuje view count NA LOKALNOJ REPLICI.
     */
    @Transactional
    public void incrementViewCount(Long videoId) {
        LOG.info("[{}] Incrementing view count for video {}", replicaId, videoId);

        replicaRegistry.ensureTableExists(replicaId);

        ViewCountReplica viewCount = replicaDAO
                .findByVideoIdForUpdate(videoId, replicaId)
                .orElseGet(() -> {
                    LOG.info("[{}] Creating new ViewCount entry for video {}", replicaId, videoId);
                    return replicaDAO.create(videoId, replicaId);
                });

        long newCount = viewCount.getCount() + 1;
        replicaDAO.update(videoId, newCount, replicaId);

        LOG.info("[{}] View count incremented to {} for video {}",
                replicaId, newCount, videoId);

        if (pushEnabled) {
            try {
                syncService.pushUpdateToOtherReplicas(videoId);
            } catch (Exception e) {
                LOG.warn("[{}] Failed to push update to other replicas: {}",
                        replicaId, e.getMessage());
            }
        }
    }

    /**
     * Vraća UKUPAN broj pregleda koristeći G-Counter MERGE.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCount(Long videoId) {
        LOG.debug("[{}] Getting total view count for video {} (with sync)", replicaId, videoId);

        try {
            syncService.pullAndMergeFromOtherReplicas(videoId);
        } catch (Exception e) {
            LOG.warn("[{}] Pull sync failed, using local data: {}", replicaId, e.getMessage());
        }

        List<String> allReplicaIds = replicaRegistry.getAllReplicaIds();
        List<ViewCountReplica> allViewCounts = replicaDAO.findAllByVideoId(videoId, allReplicaIds);

        if (allViewCounts.isEmpty()) {
            LOG.debug("[{}] No view counts found for video {}", replicaId, videoId);
            return 0L;
        }

        GCounter merged = new GCounter();
        for (ViewCountReplica vc : allViewCounts) {
            merged.increment(vc.getReplicaId(), vc.getCount());
        }

        long totalCount = merged.getValue();
        LOG.debug("[{}] Total view count for video {}: {} (from {} replicas)",
                replicaId, videoId, totalCount, allViewCounts.size());

        return totalCount;
    }

    /**
     * Vraća ukupan broj pregleda BEZ sinhronizacije.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCountNoSync(Long videoId) {
        LOG.debug("[{}] Getting total view count for video {} (no sync)", replicaId, videoId);

        List<String> allReplicaIds = replicaRegistry.getAllReplicaIds();
        List<ViewCountReplica> allViewCounts = replicaDAO.findAllByVideoId(videoId, allReplicaIds);

        if (allViewCounts.isEmpty()) {
            return 0L;
        }

        GCounter merged = new GCounter();
        for (ViewCountReplica vc : allViewCounts) {
            merged.increment(vc.getReplicaId(), vc.getCount());
        }

        return merged.getValue();
    }

    @Transactional(readOnly = true)
    public long getLocalViewCount(Long videoId) {
        return replicaDAO
                .findByVideoId(videoId, replicaId)
                .map(ViewCountReplica::getCount)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public List<ViewCountReplica> getAllReplicaViewCounts(Long videoId) {
        List<String> allReplicaIds = replicaRegistry.getAllReplicaIds();
        return replicaDAO.findAllByVideoId(videoId, allReplicaIds);
    }

    @Transactional(readOnly = true)
    public List<String> getActiveReplicaIds() {
        return replicaRegistry.getAllReplicaIds();
    }
}