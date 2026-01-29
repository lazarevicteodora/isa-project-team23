package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rs.ac.uns.ftn.isa.isa_project.crdt.GCounter;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica1;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica2;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplica1Repository;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplica2Repository;

@Service
public class CRDTViewCountService {

    @Autowired
    private ViewCountReplica1Repository replica1Repository;

    @Autowired
    private ViewCountReplica2Repository replica2Repository;

    @Autowired
    private ReplicaSyncService syncService;

    @Value("${crdt.replica.id}")
    private String replicaId;

    @Value("${crdt.sync.push-enabled:true}")
    private boolean pushEnabled;

    /**
     * Inkrementuje view count NA LOKALNOJ REPLICI sa pesimističkim lock-om
     */
    @Transactional
    public void incrementViewCount(Long videoId) {
        if ("replica-1".equals(replicaId)) {
            System.out.println("✅ REPLICA-1 branch entered!");

            ViewCountReplica1 viewCount = replica1Repository.findByVideoIdForUpdate(videoId)
                    .orElseGet(() -> {
                        ViewCountReplica1 newVC = new ViewCountReplica1(videoId);
                        ViewCountReplica1 saved = replica1Repository.save(newVC);
                        return saved;
                    });

            viewCount.increment();
            ViewCountReplica1 savedEntity = replica1Repository.save(viewCount);

            //Push update drugim replikama (asinhrono)
            if (pushEnabled) {
                syncService.pushUpdateToOtherReplicas(videoId);
            }

        } else if ("replica-2".equals(replicaId)) {
            System.out.println("✅ REPLICA-2 branch entered!");

            ViewCountReplica2 viewCount = replica2Repository.findByVideoIdForUpdate(videoId)
                    .orElseGet(() -> {
                        ViewCountReplica2 newVC = new ViewCountReplica2(videoId);
                        ViewCountReplica2 saved = replica2Repository.save(newVC);
                        return saved;
                    });

            viewCount.increment();

            ViewCountReplica2 savedEntity = replica2Repository.save(viewCount);

            //Push update drugim replikama (asinhrono)
            if (pushEnabled) {
                syncService.pushUpdateToOtherReplicas(videoId);
            }

        } else {
            System.out.println("❌ UNKNOWN REPLICA ID: " + replicaId);
        }

    }
    /**Vraća UKUPAN broj pregleda koristeći G-Counter MERGE
     *Prije vraćanja vrednosti, prvo izvršava PULL sinhronizaciju
     *sa drugim replikama da bi obezbjedio najnovije podatke
     */
    @Transactional(readOnly = true)
    public long getTotalViewCount(Long videoId) {

        syncService.pullAndMergeFromOtherReplicas(videoId);

        // Pročitaj iz obe tabele
        Long count1 = replica1Repository.findByVideoId(videoId)
                .map(ViewCountReplica1::getCount)
                .orElse(0L);

        Long count2 = replica2Repository.findByVideoId(videoId)
                .map(ViewCountReplica2::getCount)
                .orElse(0L);

        // Kreiraj G-Counter za svaku repliku
        GCounter counter1 = new GCounter();
        counter1.increment("replica-1", count1);

        GCounter counter2 = new GCounter();
        counter2.increment("replica-2", count2);

        // MERGE - ključni deo CRDT-a!
        GCounter merged = counter1.merge(counter2);

        return merged.getValue();
    }

    /**
     * Vraća ukupan broj pregleda BEZ sinhronizacije.
     * Koristi se kada nam treba brz pristup bez garancije svežih podataka.
     */
    @Transactional(readOnly = true)
    public long getTotalViewCountNoSync(Long videoId) {
        Long count1 = replica1Repository.findByVideoId(videoId)
                .map(ViewCountReplica1::getCount)
                .orElse(0L);

        Long count2 = replica2Repository.findByVideoId(videoId)
                .map(ViewCountReplica2::getCount)
                .orElse(0L);

        GCounter counter1 = new GCounter();
        counter1.increment("replica-1", count1);

        GCounter counter2 = new GCounter();
        counter2.increment("replica-2", count2);

        GCounter merged = counter1.merge(counter2);

        return merged.getValue();
    }

}