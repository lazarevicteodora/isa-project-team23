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

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRDT View Count Service - Odvojene tabele BEZ hardkodovanja!
 *
 * Svaka replika ima SVOJU tabelu, ali kod dinamički radi sa bilo kojim brojem replika.
 */
@Service
public class CRDTViewCountService {

    private static final Logger LOG = LoggerFactory.getLogger(CRDTViewCountService.class);

    @Autowired
    private ViewCountRepository viewCountRepository;

    @Autowired
    private ReplicaSyncService syncService;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.replica.urls}")
    private List<String> replicaUrls;

    @Value("${crdt.sync.push-enabled:true}")
    private boolean pushEnabled;

    /**
     * Automatski kreira tabelu za trenutnu repliku prilikom startup-a.
     */
    @PostConstruct
    public void init() {
        LOG.info("[{}] Initializing CRDT View Count Service", replicaId);

        // Kreiraj tabelu za ovu repliku ako ne postoji
        viewCountRepository.createTableIfNotExists(replicaId);

        LOG.info("[{}] Table created/verified for replica", replicaId);
    }

    /**
     * Inkrementuje view count NA LOKALNOJ TABELI te replike.
     *
     * NEMA if-else logike!
     * Dinamički koristi tabelu zasnovanu na replicaId.
     */
    @Transactional
    public void incrementViewCount(Long videoId) {
        LOG.debug("[{}] Incrementing view count for video {}", replicaId, videoId);

        // 1. Pronađi ili kreiraj zapis u tabeli TRENUTNE replike
        ViewCount viewCount = viewCountRepository
                .findByVideoIdForUpdate(videoId, replicaId)
                .orElseGet(() -> {
                    LOG.info("[{}] Creating new view count entry for video {}", replicaId, videoId);
                    return new ViewCount(videoId, replicaId);
                });

        // 2. Inkrementuj
        viewCount.increment();
        viewCountRepository.save(viewCount);

        LOG.debug("[{}] View count incremented for video {}: new count = {}",
                replicaId, videoId, viewCount.getCount());

        // 3. Push update drugim replikama
        if (pushEnabled) {
            syncService.pushUpdateToOtherReplicas(videoId);
        }
    }

    /**
     * Vraća UKUPAN broj pregleda koristeći G-Counter MERGE.
     *
     * Čita iz SVIHtabela (view_counts_replica_1, view_counts_replica_2, ...)
     * i merge-uje ih koristeći CRDT princip.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCount(Long videoId) {
        LOG.debug("[{}] Getting total view count for video {}", replicaId, videoId);

        // 1. Pull sinhronizacija
        syncService.pullAndMergeFromOtherReplicas(videoId);

        // 2. Dobavi listu svih poznatih replika
        List<String> allReplicaIds = getAllKnownReplicaIds();

        // 3. Učitaj SVE ViewCount zapise za ovaj video (iz svih tabela)
        List<ViewCount> allReplicas = viewCountRepository.findAllByVideoId(videoId, allReplicaIds);

        LOG.debug("[{}] Found {} replica entries for video {}", replicaId, allReplicas.size(), videoId);

        // 4. Kreiraj G-Counter iz svih replika
        Map<String, Long> counts = new HashMap<>();
        for (ViewCount vc : allReplicas) {
            counts.put(vc.getReplicaId(), vc.getCount());
            LOG.trace("[{}] Replica {}: {} views", replicaId, vc.getReplicaId(), vc.getCount());
        }

        GCounter counter = new GCounter(counts);
        long total = counter.getValue();

        LOG.info("[{}] Total view count for video {}: {} (from {} replicas)",
                replicaId, videoId, total, allReplicas.size());

        return total;
    }

    /**
     * Vraća listu svih poznatih replika u sistemu.
     * Ovo uključuje trenutnu repliku + sve replike iz konfiguracije.
     */
    private List<String> getAllKnownReplicaIds() {
        List<String> allIds = new java.util.ArrayList<>();

        // Dodaj trenutnu repliku
        allIds.add(replicaId);

        // Ekstraktuj replica ID-ove iz URL-ova
        // Npr. "http://backend1:8080" -> pretpostavljamo "replica-1"
        // ALI, bolje je imati eksplicitnu konfiguraciju
        for (int i = 1; i <= replicaUrls.size() + 1; i++) {
            String id = "replica-" + i;
            if (!allIds.contains(id)) {
                allIds.add(id);
            }
        }

        return allIds;
    }

    /**
     * Vraća view count za TRENUTNU repliku (bez merging-a).
     */
    @Transactional(readOnly = true)
    public long getLocalViewCount(Long videoId) {
        return viewCountRepository
                .findByVideoIdAndReplicaId(videoId, replicaId)
                .map(ViewCount::getCount)
                .orElse(0L);
    }
}