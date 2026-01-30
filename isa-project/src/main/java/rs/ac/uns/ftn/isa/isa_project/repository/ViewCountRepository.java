package rs.ac.uns.ftn.isa.isa_project.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCount;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountId;

import java.util.List;
import java.util.Optional;

/**
 * Repository za ViewCount entitet.

 * Ključne metode:
 * - findByVideoIdAndReplicaId() - pronalazi zapis za specifičnu repliku
 * - findAllByVideoId() - vraća SVE replike za dati video (za merge)
 * - findByVideoIdAndReplicaIdForUpdate() - pessimistic lock za increment
 */
@Repository
public interface ViewCountRepository extends JpaRepository<ViewCount, ViewCountId> {

    /**
     * Pronalazi ViewCount za specifičan video i repliku.
     *
     * @param videoId ID videa
     * @param replicaId ID replike (npr. "replica-1", "replica-2", ...)
     * @return Optional sa ViewCount ako postoji
     */
    Optional<ViewCount> findByVideoIdAndReplicaId(Long videoId, String replicaId);

    /**
     * Vraća SVE replike za dati video.
     * Ovo se koristi prilikom MERGE-a da bi se prikupili brojači svih replika.
     *
     * Primer:
     * Video 29 ima zapise:
     * - (29, "replica-1", 150)
     * - (29, "replica-2", 120)
     * - (29, "replica-3", 80)
     *
     * Metoda vraća listu sva 3 zapisa.
     *
     * @param videoId ID videa
     * @return Lista svih ViewCount zapisa za taj video
     */
    List<ViewCount> findAllByVideoId(Long videoId);

    /**
     * Pronalazi ViewCount sa pesimističkim lock-om.
     * Koristi se prilikom inkrementovanja da spreči race conditions.
     *
     * @param videoId ID videa
     * @param replicaId ID replike
     * @return Optional sa zakljuÄ‡anim ViewCount zapisom
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vc FROM ViewCount vc WHERE vc.videoId = :videoId AND vc.replicaId = :replicaId")
    Optional<ViewCount> findByVideoIdAndReplicaIdForUpdate(
            @Param("videoId") Long videoId,
            @Param("replicaId") String replicaId
    );

    /**
     * Brise sve zapise za dati video.
     * Korisno za cleanup prilikom brisanja videa.
     *
     * @param videoId ID videa
     */
    void deleteAllByVideoId(Long videoId);

    /**
     * Broji koliko različitih replika ima zapise za dati video.
     *
     * @param videoId ID videa
     * @return Broj različitih replika
     */
    @Query("SELECT COUNT(DISTINCT vc.replicaId) FROM ViewCount vc WHERE vc.videoId = :videoId")
    long countDistinctReplicasByVideoId(@Param("videoId") Long videoId);
}