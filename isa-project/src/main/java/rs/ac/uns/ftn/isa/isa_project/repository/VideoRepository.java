package rs.ac.uns.ftn.isa.isa_project.repository;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.User;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;


@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {


    List<Video> findByAuthor(User author);

    List<Video> findByAuthorOrderByCreatedAtDesc(User author);


    List<Video> findAllByOrderByCreatedAtDesc();

    @Query("SELECT v FROM Video v JOIN v.tags t WHERE t = :tag ORDER BY v.createdAt DESC")
    List<Video> findByTag(@Param("tag") String tag);

    List<Video> findByCreatedAtAfter(LocalDateTime date);

    long countByAuthor(User author);

    Optional<Video> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Video v WHERE v.id = :id")
    Optional<Video> findByIdForUpdate(@Param("id") Long id);
}
