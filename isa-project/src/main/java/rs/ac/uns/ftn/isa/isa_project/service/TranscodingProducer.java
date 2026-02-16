package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.isa.isa_project.dto.TranscodingJobDTO;
import rs.ac.uns.ftn.isa.isa_project.model.TranscodingJob;
import rs.ac.uns.ftn.isa.isa_project.model.Video;
import rs.ac.uns.ftn.isa.isa_project.repository.TranscodingJobRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Producer servis - šalje transcoding zadatke u RabbitMQ queue.
 *
 * Poziva se nakon što je video uploadovan i sacuvan u bazu.
 */
@Service
public class TranscodingProducer {

    private static final Logger LOG = LoggerFactory.getLogger(TranscodingProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TranscodingJobRepository transcodingJobRepository;

    @Value("${rabbitmq.transcoding.exchange:video.transcoding.exchange}")
    private String transcodingExchange;

    @Value("${rabbitmq.transcoding.routingkey:video.transcoding}")
    private String transcodingRoutingKey;

    // Predefinisane rezolucije za transcoding
    @Value("${transcoding.resolutions:720p,480p,360p}")
    private String[] defaultResolutions;

    @Value("${transcoding.output.directory:storage/transcoded}")
    private String outputDirectory;

    /**
     * Šalje video na transcoding sa default rezolucijama.
     *
     * @param video Video objekat koji treba transcode-ovati
     * @return Job ID za praćenje statusa
     */
    public String sendVideoForTranscoding(Video video) {
        return sendVideoForTranscoding(video, Arrays.asList(defaultResolutions));
    }

    /**
     * Šalje video na transcoding sa specifičnim rezolucijama.
     *
     * @param video Video objekat
     * @param targetResolutions Lista target rezolucija (npr. ["720p", "480p"])
     * @return Job ID za praćenje statusa
     */
    public String sendVideoForTranscoding(Video video, List<String> targetResolutions) {
        LOG.info("Sending video {} for transcoding. Resolutions: {}",
                video.getId(), targetResolutions);

        try {
            // Generiši jedinstveni job ID
            String jobId = UUID.randomUUID().toString();

            // Kreiraj TranscodingJob u bazi
            TranscodingJob job = new TranscodingJob(jobId, video, video.getVideoPath());
            transcodingJobRepository.save(job);

            // Kreiraj DTO poruku za queue
            TranscodingJobDTO dto = new TranscodingJobDTO(
                    video.getId(),
                    video.getVideoPath(),
                    outputDirectory,
                    targetResolutions,
                    jobId
            );

            // Pošalji poruku u RabbitMQ queue
            rabbitTemplate.convertAndSend(
                    transcodingExchange,
                    transcodingRoutingKey,
                    dto
            );

            LOG.info("Transcoding job {} sent to queue successfully", jobId);
            return jobId;

        } catch (Exception e) {
            LOG.error("Failed to send video for transcoding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to queue transcoding job", e);
        }
    }

    /**
     * Re-send neuspelog job-a (retry).
     *
     * @param jobId ID job-a koji treba retry-ovati
     */
    public void retryTranscodingJob(String jobId) {
        LOG.info("Retrying transcoding job: {}", jobId);

        TranscodingJob job = transcodingJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Transcoding job not found: " + jobId));

        // Kreiraj DTO od postojećeg job-a
        TranscodingJobDTO dto = new TranscodingJobDTO(
                job.getVideo().getId(),
                job.getOriginalPath(),
                outputDirectory,
                Arrays.asList(defaultResolutions), // Možeš čuvati i u bazi koje rezolucije su tražene
                jobId
        );

        // Pošalji ponovo u queue
        rabbitTemplate.convertAndSend(
                transcodingExchange,
                transcodingRoutingKey,
                dto
        );

        LOG.info("Transcoding job {} re-queued successfully", jobId);
    }
}