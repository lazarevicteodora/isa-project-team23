package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCount;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CRDTViewCountService {

    private static final Logger LOG = LoggerFactory.getLogger(CRDTViewCountService.class);

    @Autowired
    private ViewCountRepository viewCountRepository;

    @Autowired
    private ReplicaSyncService syncService;

    @Value("${crdt.replica.id}")
    private String replicaId;  // Npr. "replica-1", "replica-2", "replica-3"...

    @Value("${crdt.sync.push-enabled:true}")
    private boolean pushEnabled;

    /**
     * Inkrementuje view count NA LOKALNOJ REPLICI sa pesimističkim lock-om.
     */
    @Transactional
    public void incrementViewCount(Long videoId) {
        LOG.debug("[{}] Incrementing view count for video {}", replicaId, videoId);

        // 1. Pronađi ili kreiraj zapis za TRENUTNU repliku
        ViewCount viewCount = viewCountRepository
                .findByVideoIdAndReplicaIdForUpdate(videoId, replicaId)
                .orElseGet(() -> {
                    LOG.info("[{}] Creating new view count entry for video {}", replicaId, videoId);
                    ViewCount newVC = new ViewCount(videoId, replicaId);
                    return viewCountRepository.save(newVC);
                });

        // 2. Inkrementuj brojač
        viewCount.increment();
        viewCountRepository.save(viewCount);

        LOG.debug("[{}] View count incremented for video {}: new count = {}",
                replicaId, videoId, viewCount.getCount());

        // 3. Push update drugim replikama (asinhrono, opciono)
        if (pushEnabled) {
            syncService.pushUpdateToOtherReplicas(videoId);
        }
    }

    /**
     * Vraća UKUPAN broj pregleda koristeći G-Counter MERGE.
     * Pre vraćanja vrednosti, prvo izvršava PULL sinhronizaciju sa drugim replikama.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCount(Long videoId) {
        LOG.debug("[{}] Getting total view count for video {}", replicaId, videoId);

        // 1. Pull sinhronizacija - dobavi najnovije podatke od drugih replika
        syncService.pullAndMergeFromOtherReplicas(videoId);

        // 2. Ucitaj SVE replike za ovaj video
        List<ViewCount> allReplicas = viewCountRepository.findAllByVideoId(videoId);

        LOG.debug("[{}] Found {} replica entries for video {}", replicaId, allReplicas.size(), videoId);

        // 3. Kreiraj G-Counter iz svih replika
        Map<String, Long> counts = new HashMap<>();
        for (ViewCount vc : allReplicas) {
            counts.put(vc.getReplicaId(), vc.getCount());
            LOG.trace("[{}] Replica {}: {} views", replicaId, vc.getReplicaId(), vc.getCount());
        }

        GCounter counter = new GCounter(counts);

        // 4. MERGE - saberi sve replike (CRDT operacija)
        long total = counter.getValue();

        LOG.info("[{}] Total view count for video {}: {} (from {} replicas)",
                replicaId, videoId, total, allReplicas.size());

        return total;
    }

    /**
     * Vraća ukupan broj pregleda BEZ sinhronizacije.
     * Koristi se kada nam treba brz pristup bez garancije svežih podataka.
     *
     * Takođe dinamički - radi sa bilo kojim brojem replika.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCountNoSync(Long videoId) {
        LOG.debug("[{}] Getting total view count (no sync) for video {}", replicaId, videoId);

        // Ucitaj SVE replike
        List<ViewCount> allReplicas = viewCountRepository.findAllByVideoId(videoId);

        // Kreiraj G-Counter
        Map<String, Long> counts = new HashMap<>();
        for (ViewCount vc : allReplicas) {
            counts.put(vc.getReplicaId(), vc.getCount());
        }

        GCounter counter = new GCounter(counts);
        long total = counter.getValue();

        LOG.debug("[{}] Total view count (no sync) for video {}: {}", replicaId, videoId, total);

        return total;
    }

    /**
     * Vraća broj različitih replika koje imaju zapise za dati video.
     * Korisno za monitoring i debugging.
     */
    public long getReplicaCount(Long videoId) {
        return viewCountRepository.countDistinctReplicasByVideoId(videoId);
    }

    /**
     * Vraća view count za TRENUTNU repliku (bez merging-a).
     * Korisno za debugging i monitoring pojedinačnih replika.
     */
    @Transactional(readOnly = true)
    public long getLocalViewCount(Long videoId) {
        return viewCountRepository
                .findByVideoIdAndReplicaId(videoId, replicaId)
                .map(ViewCount::getCount)
                .orElse(0L);
    }
}