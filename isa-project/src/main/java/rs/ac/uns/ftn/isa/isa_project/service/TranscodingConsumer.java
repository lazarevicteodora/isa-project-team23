package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.isa.isa_project.dto.TranscodingJobDTO;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingJob;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingStatus;
import rs.ac.uns.ftn.isa.isa_project.repository.TranscodingJobRepository;

import java.net.InetAddress;
import java.util.Map;

/**
 * Consumer servis - sluša RabbitMQ queue i procesira transcoding zadatke.
 */
@Component
public class TranscodingConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodingConsumer.class);

    @Autowired
    private FFmpegService ffmpegService;

    @Autowired
    private TranscodingJobRepository transcodingJobRepository;

    @Value("${consumer.id:consumer-unknown}")
    private String consumerId;

    /**
     * Listener metoda koja prima poruke iz queue-a.
     */
    @RabbitListener(queues = "${rabbitmq.transcoding.queue:video.transcoding.queue}")
    @Transactional
    public void handleTranscodingJob(TranscodingJobDTO jobDTO) {
        LOG.info("[{}] Received transcoding job: {}", consumerId, jobDTO.getJobId());

        TranscodingJob job = null;

        try {
            // 1. Nađi job u bazi i označi kao PROCESSING
            job = transcodingJobRepository.findByJobId(jobDTO.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobDTO.getJobId()));

            job.markAsProcessing(getConsumerIdentifier());
            transcodingJobRepository.save(job);

            LOG.info("[{}] Starting transcoding for video {} (Job: {})",
                    consumerId, jobDTO.getVideoId(), jobDTO.getJobId());

            // 2. Provjeri da li je FFmpeg instaliran
            if (!ffmpegService.isFFmpegInstalled()) {
                throw new RuntimeException("FFmpeg is not installed on this consumer!");
            }

            // 3. Transcode video u sve tražene rezolucije
            Map<String, String> results = ffmpegService.transcodeMultipleResolutions(
                    jobDTO.getOriginalVideoPath(),
                    jobDTO.getOutputDirectory(),
                    jobDTO.getTargetResolutions()
            );

            // 4. Provjeri rezultate
            if (results.isEmpty()) {
                throw new RuntimeException("All transcoding attempts failed!");
            }

            // 5. Sačuvaj output paths
            for (Map.Entry<String, String> entry : results.entrySet()) {
                job.addOutputPath(entry.getValue());
                LOG.info("[{}] Transcoded to {}: {}", consumerId, entry.getKey(), entry.getValue());
            }

            // 6. Označi job kao completed
            job.markAsCompleted();
            transcodingJobRepository.save(job);

            LOG.info("[{}] Transcoding job {} completed successfully. Output files: {}",
                    consumerId, jobDTO.getJobId(), results.size());

        } catch (Exception e) {
            LOG.error("[{}] Transcoding job {} failed: {}",
                    consumerId, jobDTO.getJobId(), e.getMessage(), e);

            // Označi job kao failed
            if (job != null) {
                job.markAsFailed(e.getMessage());
                transcodingJobRepository.save(job);
            }

            // Re-throw exception da RabbitMQ pošalje poruku u DLQ
            throw new RuntimeException("Transcoding failed", e);
        }
    }

    /**
     * Generiše jedinstveni identifikator za ovog consumer-a.
     * Kombinuje konfigurisani consumer ID i hostname/IP.
     */
    private String getConsumerIdentifier() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return consumerId + "@" + hostname;
        } catch (Exception e) {
            return consumerId;
        }
    }
}