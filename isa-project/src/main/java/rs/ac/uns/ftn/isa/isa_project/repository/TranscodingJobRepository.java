package rs.ac.uns.ftn.isa.isa_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingJob;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, Long> {

    Optional<TranscodingJob> findByJobId(String jobId);

    List<TranscodingJob> findByVideoId(Long videoId);

    List<TranscodingJob> findByStatus(TranscodingStatus status);

    List<TranscodingJob> findByConsumerId(String consumerId);
}