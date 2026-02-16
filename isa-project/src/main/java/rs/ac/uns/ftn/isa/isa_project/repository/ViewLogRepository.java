package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.ViewLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewLogRepository extends JpaRepository<ViewLog, Long> {

    Optional<ViewLog> findByVideoIdAndViewDate(Long videoId, LocalDate viewDate);

    // Vraća sve view logove za poslednjih 7 dana
    @Query("SELECT vl FROM ViewLog vl WHERE vl.viewDate >= :since ORDER BY vl.viewDate DESC")
    List<ViewLog> findAllSince(@Param("since") LocalDate since);

    // Vraća view logove za konkretan video u poslednjih 7 dana
    @Query("SELECT vl FROM ViewLog vl WHERE vl.video.id = :videoId AND vl.viewDate >= :since")
    List<ViewLog> findByVideoIdSince(@Param("videoId") Long videoId, @Param("since") LocalDate since);
}