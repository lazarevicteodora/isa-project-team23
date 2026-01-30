package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCount;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository koji koristi JdbcTemplate za rad sa dinamičkim tabelama.
 *
 * Umesto JpaRepository, koristimo ručne SQL upite gde možemo
 * dinamički ubaciti ime tabele.
 */
@Repository
public class ViewCountRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Vraća ime tabele za datu repliku.
     * Format: view_counts_replica_1, view_counts_replica_2, ...
     */
    private String getTableName(String replicaId) {
        // Konvertuj "replica-1" -> "replica_1" (zameni - sa _)
        String sanitized = replicaId.replace("-", "_");
        return "view_counts_" + sanitized;
    }

    /**
     * RowMapper za mapiranje ResultSet-a u ViewCount objekat.
     */
    private RowMapper<ViewCount> viewCountRowMapper(String replicaId) {
        return (ResultSet rs, int rowNum) -> {
            ViewCount vc = new ViewCount();
            vc.setVideoId(rs.getLong("video_id"));
            vc.setCount(rs.getLong("count"));
            vc.setReplicaId(replicaId);
            return vc;
        };
    }

    /**
     * Pronalazi ViewCount za dati video i repliku.
     */
    public Optional<ViewCount> findByVideoIdAndReplicaId(Long videoId, String replicaId) {
        String tableName = getTableName(replicaId);
        String sql = String.format("SELECT * FROM %s WHERE video_id = ?", tableName);

        List<ViewCount> results = jdbcTemplate.query(
                sql,
                viewCountRowMapper(replicaId),
                videoId
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Čuva ili update-uje ViewCount.
     */
    public ViewCount save(ViewCount viewCount) {
        String tableName = getTableName(viewCount.getReplicaId());

        // Pokušaj UPDATE prvo
        String updateSql = String.format(
                "UPDATE %s SET count = ? WHERE video_id = ?",
                tableName
        );

        int rowsAffected = jdbcTemplate.update(
                updateSql,
                viewCount.getCount(),
                viewCount.getVideoId()
        );

        // Ako nije update-ovano, uradi INSERT
        if (rowsAffected == 0) {
            String insertSql = String.format(
                    "INSERT INTO %s (video_id, count) VALUES (?, ?)",
                    tableName
            );
            jdbcTemplate.update(
                    insertSql,
                    viewCount.getVideoId(),
                    viewCount.getCount()
            );
        }

        return viewCount;
    }

    /**
     * Pronalazi ViewCount sa pesimističkim lock-om (FOR UPDATE).
     */
    public Optional<ViewCount> findByVideoIdForUpdate(Long videoId, String replicaId) {
        String tableName = getTableName(replicaId);
        String sql = String.format(
                "SELECT * FROM %s WHERE video_id = ? FOR UPDATE",
                tableName
        );

        List<ViewCount> results = jdbcTemplate.query(
                sql,
                viewCountRowMapper(replicaId),
                videoId
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Vraća sve ViewCount zapise za dati video SA SVIH REPLIKA.
     * Ovo se koristi za G-Counter merge.
     */
    public List<ViewCount> findAllByVideoId(Long videoId, List<String> allReplicaIds) {
        List<ViewCount> allCounts = new java.util.ArrayList<>();

        for (String replicaId : allReplicaIds) {
            try {
                String tableName = getTableName(replicaId);
                String sql = String.format("SELECT * FROM %s WHERE video_id = ?", tableName);

                List<ViewCount> results = jdbcTemplate.query(
                        sql,
                        viewCountRowMapper(replicaId),
                        videoId
                );

                allCounts.addAll(results);
            } catch (Exception e) {
                // Tabela možda ne postoji - to je OK, preskačemo
            }
        }

        return allCounts;
    }

    /**
     * Kreira novu tabelu za repliku ako ne postoji.
     */
    public void createTableIfNotExists(String replicaId) {
        String tableName = getTableName(replicaId);

        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                video_id BIGINT NOT NULL PRIMARY KEY,
                count BIGINT NOT NULL DEFAULT 0,
                CONSTRAINT fk_%s_video FOREIGN KEY (video_id) 
                    REFERENCES videos(id) ON DELETE CASCADE
            )
            """, tableName, tableName);

        jdbcTemplate.execute(createTableSql);

        // Kreiraj indeks
        String createIndexSql = String.format(
                "CREATE INDEX IF NOT EXISTS idx_%s_video ON %s(video_id)",
                tableName, tableName
        );

        jdbcTemplate.execute(createIndexSql);
    }
}