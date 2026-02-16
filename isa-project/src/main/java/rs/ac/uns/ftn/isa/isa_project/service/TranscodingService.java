package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingJob;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingStatus;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.TranscodingJobRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business logic servis za transcoding operacije.
 * Pruža API za controller i ostale servise.
 */
@Service
public class TranscodingService {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodingService.class);

    @Autowired
    private TranscodingJobRepository transcodingJobRepository;

    @Autowired
    private TranscodingProducer transcodingProducer;

    /**
     * Inicira transcoding za video.
     *
     * @param video Video koji treba transcode-ovati
     * @return Job ID
     */
    @Transactional
    public String startTranscoding(Video video) {
        LOG.info("Starting transcoding for video {}", video.getId());
        return transcodingProducer.sendVideoForTranscoding(video);
    }

    /**
     * Inicira transcoding sa custom rezolucijama.
     */
    @Transactional
    public String startTranscoding(Video video, List<String> resolutions) {
        LOG.info("Starting transcoding for video {} with resolutions: {}",
                video.getId(), resolutions);
        return transcodingProducer.sendVideoForTranscoding(video, resolutions);
    }

    /**
     * Dobavlja status transcoding job-a.
     */
    @Transactional(readOnly = true)
    public Optional<TranscodingJob> getJobStatus(String jobId) {
        return transcodingJobRepository.findByJobId(jobId);
    }

    /**
     * Dobavlja sve transcoding job-ove za određeni video.
     */
    @Transactional(readOnly = true)
    public List<TranscodingJob> getJobsForVideo(Long videoId) {
        return transcodingJobRepository.findByVideoId(videoId);
    }

    /**
     * Dobavlja sve job-ove sa određenim statusom.
     */
    @Transactional(readOnly = true)
    public List<TranscodingJob> getJobsByStatus(TranscodingStatus status) {
        return transcodingJobRepository.findByStatus(status);
    }

    /**
     * Dobavlja sve pending job-ove (čekaju u queue-u).
     */
    @Transactional(readOnly = true)
    public List<TranscodingJob> getPendingJobs() {
        return transcodingJobRepository.findByStatus(TranscodingStatus.PENDING);
    }

    /**
     * Dobavlja sve failed job-ove.
     */
    @Transactional(readOnly = true)
    public List<TranscodingJob> getFailedJobs() {
        return transcodingJobRepository.findByStatus(TranscodingStatus.FAILED);
    }

    /**
     * Retry neuspelog job-a.
     */
    @Transactional
    public void retryJob(String jobId) {
        LOG.info("Retrying transcoding job: {}", jobId);

        TranscodingJob job = transcodingJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (job.getStatus() != TranscodingStatus.FAILED) {
            throw new RuntimeException("Can only retry failed jobs");
        }

        // Resetuj status
        job.setStatus(TranscodingStatus.PENDING);
        job.setErrorMessage(null);
        transcodingJobRepository.save(job);

        // Pošalji ponovo u queue
        transcodingProducer.retryTranscodingJob(jobId);
    }

    /**
     * Otkazuje job koji čeka u queue-u.
     */
    @Transactional
    public void cancelJob(String jobId) {
        LOG.info("Cancelling transcoding job: {}", jobId);

        TranscodingJob job = transcodingJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (job.getStatus() == TranscodingStatus.PROCESSING) {
            throw new RuntimeException("Cannot cancel job that is already processing");
        }

        job.setStatus(TranscodingStatus.CANCELLED);
        transcodingJobRepository.save(job);
    }

    /**
     * Dobavlja statistiku o transcoding job-ovima.
     */
    @Transactional(readOnly = true)
    public TranscodingStats getStats() {
        long total = transcodingJobRepository.count();
        long pending = transcodingJobRepository.findByStatus(TranscodingStatus.PENDING).size();
        long processing = transcodingJobRepository.findByStatus(TranscodingStatus.PROCESSING).size();
        long completed = transcodingJobRepository.findByStatus(TranscodingStatus.COMPLETED).size();
        long failed = transcodingJobRepository.findByStatus(TranscodingStatus.FAILED).size();

        return new TranscodingStats(total, pending, processing, completed, failed);
    }

    /**
     * DTO za statistiku.
     */
    public static class TranscodingStats {
        public final long total;
        public final long pending;
        public final long processing;
        public final long completed;
        public final long failed;

        public TranscodingStats(long total, long pending, long processing, long completed, long failed) {
            this.total = total;
            this.pending = pending;
            this.processing = processing;
            this.completed = completed;
            this.failed = failed;
        }
    }
}