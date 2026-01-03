package rs.ac.uns.ftn.isa.isa_project.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interfejs za Video entitet.
 * Spring Data JPA automatski generiše implementaciju.
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * Pronalazi sve videe određenog autora.
     *
     * @param author Korisnik čije videe tražimo
     * @return Lista videa tog korisnika
     */
    List<Video> findByAuthor(User author);

    /**
     * Pronalazi sve videe određenog autora, sortirane po vremenu (najnoviji prvi).
     *
     * @param author Korisnik čije videe tražimo
     * @return Lista videa sortiranih od najnovijih ka najstarijim
     */
    List<Video> findByAuthorOrderByCreatedAtDesc(User author);

    /**
     * Pronalazi sve videe sortirane po vremenu kreiranja (najnoviji prvi).
     * Koristi se za prikaz home page-a.
     *
     * @return Lista svih videa sortiranih od najnovijih ka najstarijim
     */
    List<Video> findAllByOrderByCreatedAtDesc();

    /**
     * Pronalazi videe koji sadrže određeni tag.
     *
     * @param tag Tag koji tražimo
     * @return Lista videa koji imaju taj tag
     */
    @Query("SELECT v FROM Video v JOIN v.tags t WHERE t = :tag ORDER BY v.createdAt DESC")
    List<Video> findByTag(@Param("tag") String tag);

    /**
     * Pronalazi videe kreirane nakon određenog datuma.
     * Korisno za filtriranje po vremenu.
     *
     * @param date Datum nakon kojeg tražimo videe
     * @return Lista videa kreiranih nakon datog datuma
     */
    List<Video> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Broji koliko videa ima određeni korisnik.
     *
     * @param author Korisnik čije videe brojimo
     * @return Broj videa tog korisnika
     */
    long countByAuthor(User author);
}
