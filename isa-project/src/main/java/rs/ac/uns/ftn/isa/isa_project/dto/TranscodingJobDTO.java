package rs.ac.uns.ftn.isa.isa_project.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO koji se šalje preko RabbitMQ queue-a za transcoding video fajlova.
 * Sadrži sve informacije potrebne consumer-u da obavi transcoding.
 */
public class TranscodingJobDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long videoId;
    private String originalVideoPath;
    private String outputDirectory;
    private List<String> targetResolutions; // npr. ["720p", "480p", "360p"]
    private String jobId; // Jedinstveni ID za ovaj transcoding job

    public TranscodingJobDTO() {
    }

    public TranscodingJobDTO(Long videoId, String originalVideoPath, String outputDirectory, List<String> targetResolutions, String jobId) {
        this.videoId = videoId;
        this.originalVideoPath = originalVideoPath;
        this.outputDirectory = outputDirectory;
        this.targetResolutions = targetResolutions;
        this.jobId = jobId;
    }

    // Getters and Setters
    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getOriginalVideoPath() {
        return originalVideoPath;
    }

    public void setOriginalVideoPath(String originalVideoPath) {
        this.originalVideoPath = originalVideoPath;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public List<String> getTargetResolutions() {
        return targetResolutions;
    }

    public void setTargetResolutions(List<String> targetResolutions) {
        this.targetResolutions = targetResolutions;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        return "TranscodingJobDTO{" +
                "videoId=" + videoId +
                ", originalVideoPath='" + originalVideoPath + '\'' +
                ", outputDirectory='" + outputDirectory + '\'' +
                ", targetResolutions=" + targetResolutions +
                ", jobId='" + jobId + '\'' +
                '}';
    }
}