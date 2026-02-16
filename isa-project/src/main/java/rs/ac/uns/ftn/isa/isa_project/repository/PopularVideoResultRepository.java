package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.PopularVideoResult;

import java.util.List;

@Repository
public interface PopularVideoResultRepository extends JpaRepository<PopularVideoResult, Long> {

    // Vraća top 3 iz poslednjeg pokretanja pipeline-a
    @Query("""
        SELECT pvr FROM PopularVideoResult pvr
        WHERE pvr.runAt = (SELECT MAX(p.runAt) FROM PopularVideoResult p)
        ORDER BY pvr.rankPosition ASC
    """)
    List<PopularVideoResult> findLatestResults();
}