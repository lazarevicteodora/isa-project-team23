package rs.ac.uns.ftn.isa.isa_project.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Tagovi koji opisuju sadržaj videa.
     * Čuvaju se u posebnoj tabeli video_tags.
     */
    @ElementCollection
    @CollectionTable(
            name = "video_tags",
            joinColumns = @JoinColumn(name = "video_id")
    )
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    /**
     * Relativna putanja do thumbnail slike na serveru.
     */
    @Column(name = "thumbnail_path", nullable = false)
    private String thumbnailPath;

    /**
     * Relativna putanja do video fajla na serveru.
     */
    @Column(name = "video_path", nullable = false)
    private String videoPath;

    /**
     * Sistemsko vreme kreiranja objave.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Geografska lokacija - latitude (opciono).
     */
    @Column(name = "latitude")
    private Double latitude;


    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;  // Početna vrednost 0


    /**
     * Autor videa (registrovan korisnik).
     * ManyToOne - jedan korisnik može imati više videa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // ==================== Lifecycle Callbacks ====================

    /**
     * Automatski postavlja vreme kreiranja pre nego što se entitet sačuva.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Constructors ====================

    public Video() {
        super();
    }

    public Video(String title, String description, Set<String> tags,
                 String thumbnailPath, String videoPath, User author) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.author = author;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Long getViewCount() {
        return viewCount;
    }
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
    // ==================== equals, hashCode, toString ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video video = (Video) o;
        return id != null && id.equals(video.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                ", author=" + (author != null ? author.getUsername() : "null") +
                '}';
    }
}
