package rs.ac.uns.ftn.isa.isa_project.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO koji koristi Native SQL za rad sa odvojenim tabelama po replici.
 *
 * Svaka replika ima svoju tabelu:
 * - replica-1 → view_counts_replica_1
 * - replica-2 → view_counts_replica_2
 * - replica-3 → view_counts_replica_3
 */
@Repository
public class ViewCountReplicaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Konvertuje replica ID u ime tabele.
     */
    private String getTableName(String replicaId) {
        return "view_counts_" + replicaId.replace("-", "_");
    }

    /**
     * Pronalazi view count za dati video na određenoj replici.
     */
    public Optional<ViewCountReplica> findByVideoId(Long videoId, String replicaId) {
        String tableName = getTableName(replicaId);
        String sql = "SELECT id, video_id, count, last_updated FROM " + tableName +
                " WHERE video_id = :videoId";

        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("videoId", videoId);

            Object[] result = (Object[]) query.getSingleResult();

            ViewCountReplica viewCount = new ViewCountReplica();
            viewCount.setId(((Number) result[0]).longValue());
            viewCount.setVideoId(((Number) result[1]).longValue());
            viewCount.setCount(((Number) result[2]).longValue());
            viewCount.setLastUpdated(result[3] != null ? ((Number) result[3]).longValue() : null);
            viewCount.setReplicaId(replicaId);

            return Optional.of(viewCount);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Pronalazi view count sa pesimističkim lock-om za update.
     */
    @Transactional
    public Optional<ViewCountReplica> findByVideoIdForUpdate(Long videoId, String replicaId) {
        String tableName = getTableName(replicaId);
        String sql = "SELECT id, video_id, count, last_updated FROM " + tableName +
                " WHERE video_id = :videoId FOR UPDATE";

        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("videoId", videoId);

            Object[] result = (Object[]) query.getSingleResult();

            ViewCountReplica viewCount = new ViewCountReplica();
            viewCount.setId(((Number) result[0]).longValue());
            viewCount.setVideoId(((Number) result[1]).longValue());
            viewCount.setCount(((Number) result[2]).longValue());
            viewCount.setLastUpdated(result[3] != null ? ((Number) result[3]).longValue() : null);
            viewCount.setReplicaId(replicaId);

            return Optional.of(viewCount);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Kreira novi view count za dati video na replici.
     */
    @Transactional
    public ViewCountReplica create(Long videoId, String replicaId) {
        String tableName = getTableName(replicaId);
        Long timestamp = System.currentTimeMillis();

        String sql = "INSERT INTO " + tableName +
                " (video_id, count, last_updated) VALUES (:videoId, 0, :timestamp) RETURNING id";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("videoId", videoId);
        query.setParameter("timestamp", timestamp);

        Long id = ((Number) query.getSingleResult()).longValue();

        ViewCountReplica viewCount = new ViewCountReplica(videoId);
        viewCount.setId(id);
        viewCount.setLastUpdated(timestamp);
        viewCount.setReplicaId(replicaId);

        return viewCount;
    }

    /**
     * Ažurira view count za dati video na replici.
     */
    @Transactional
    public void update(Long videoId, Long newCount, String replicaId) {
        String tableName = getTableName(replicaId);
        Long timestamp = System.currentTimeMillis();

        String sql = "UPDATE " + tableName +
                " SET count = :count, last_updated = :timestamp WHERE video_id = :videoId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("count", newCount);
        query.setParameter("timestamp", timestamp);
        query.setParameter("videoId", videoId);

        query.executeUpdate();
    }

    /**
     * Vraća sve view count-ove za dati video ID (sa svih dostupnih replika).
     */
    public List<ViewCountReplica> findAllByVideoId(Long videoId, List<String> replicaIds) {
        List<ViewCountReplica> results = new ArrayList<>();

        for (String replicaId : replicaIds) {
            findByVideoId(videoId, replicaId).ifPresent(results::add);
        }

        return results;
    }

    /**
     * Proverava da li tabela za repliku postoji u bazi.
     */
    public boolean tableExists(String replicaId) {
        String tableName = getTableName(replicaId);
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables " +
                "WHERE table_name = :tableName)";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("tableName", tableName);

        return (Boolean) query.getSingleResult();
    }

    /**
     * Kreira tabelu za novu repliku ako ne postoji.
     */
    @Transactional
    public void createTableIfNotExists(String replicaId) {
        if (tableExists(replicaId)) {
            return;
        }

        String tableName = getTableName(replicaId);
        String sql = "CREATE TABLE " + tableName + " (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "video_id BIGINT NOT NULL UNIQUE, " +
                "count BIGINT NOT NULL DEFAULT 0, " +
                "last_updated BIGINT" +
                ")";

        entityManager.createNativeQuery(sql).executeUpdate();

        // Kreiraj index
        String indexSql = "CREATE INDEX idx_" + tableName.replace("view_counts_", "") +
                "_video_id ON " + tableName + "(video_id)";
        entityManager.createNativeQuery(indexSql).executeUpdate();
    }
}