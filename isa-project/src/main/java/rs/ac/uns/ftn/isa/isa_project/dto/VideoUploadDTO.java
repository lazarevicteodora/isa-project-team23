package rs.ac.uns.ftn.isa.isa_project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.isa.isa_project.validation.FileExtension;
import rs.ac.uns.ftn.isa.isa_project.validation.FileSize;
import java.util.Set;

/**
 * DTO za upload novog videa.
 * Prima podatke sa fronta u multipart/form-data formatu.
 *
 * Sadrži:
 * - Tekstualne podatke (title, description, tags)
 * - Fajlove (thumbnail, video)
 * - Geografsku lokaciju (opciono)
 */
public class VideoUploadDTO {

    @NotBlank(message = "Naslov je obavezan")
    @Size(max = 200, message = "Naslov može imati maksimalno 200 karaktera")
    private String title;

    @Size(max = 5000, message = "Opis može imati maksimalno 5000 karaktera")
    private String description;

    /**
     * Tagovi odvojeni zarezom (npr. "gaming,tutorial,java")
     * Frontend će ih slati kao String, a mi ih parsiramo u Set.
     */
    private String tags;

    @NotNull(message = "Thumbnail slika je obavezna")
    @FileExtension(allowed = {"jpg", "jpeg", "png"}, message = "Thumbnail mora biti slika")
    private MultipartFile thumbnail;

    @NotNull(message = "Video fajl je obavezan")
    @FileSize(max = 200 * 1024 * 1024, message = "Video ne sme biti veći od 200MB")
    @FileExtension(allowed = {"mp4"}, message = "Video mora biti MP4")
    private MultipartFile video;

    /**
     * Geografska lokacija - latitude (opciono)
     */
    private Double latitude;

    /**
     * Geografska lokacija - longitude (opciono)
     */
    private Double longitude;

    // ==================== Constructors ====================

    public VideoUploadDTO() {
        super();
    }

    public VideoUploadDTO(String title, String description, String tags,
                          MultipartFile thumbnail, MultipartFile video,
                          Double latitude, Double longitude) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.thumbnail = thumbnail;
        this.video = video;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ==================== Getters and Setters ====================

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Parsira tagove iz String-a u Set.
     * Primer: "gaming,tutorial,java" → Set("gaming", "tutorial", "java")
     */
    public Set<String> getParsedTags() {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        return Set.of(tags.split(","));
    }

    public MultipartFile getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(MultipartFile thumbnail) {
        this.thumbnail = thumbnail;
    }

    public MultipartFile getVideo() {
        return video;
    }

    public void setVideo(MultipartFile video) {
        this.video = video;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "VideoUploadDTO{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", thumbnailSize=" + (thumbnail != null ? thumbnail.getSize() : 0) + " bytes" +
                ", videoSize=" + (video != null ? video.getSize() : 0) + " bytes" +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}