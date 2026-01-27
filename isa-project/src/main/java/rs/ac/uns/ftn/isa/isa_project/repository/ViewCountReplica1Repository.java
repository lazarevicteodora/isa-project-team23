package rs.ac.uns.ftn.isa.isa_project.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.ViewCountReplica1;

import java.util.Optional;

@Repository
public interface ViewCountReplica1Repository extends JpaRepository<ViewCountReplica1, Long> {

    Optional<ViewCountReplica1> findByVideoId(Long videoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vc FROM ViewCountReplica1 vc WHERE vc.videoId = :videoId")
    Optional<ViewCountReplica1> findByVideoIdForUpdate(@Param("videoId") Long videoId);
}
