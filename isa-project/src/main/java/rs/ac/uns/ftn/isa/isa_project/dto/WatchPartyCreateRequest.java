package rs.ac.uns.ftn.isa.isa_project.dto;


public class WatchPartyCreateRequest {

    private Long videoId;
    private String videoTitle;

    public WatchPartyCreateRequest() {
    }

    public WatchPartyCreateRequest(Long videoId, String videoTitle) {
        this.videoId = videoId;
        this.videoTitle = videoTitle;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }
}