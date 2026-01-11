package rs.ac.uns.ftn.isa.isa_project.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "channel_id"})
)
public class Subscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private LocalDateTime subscribedAt;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @PrePersist
    protected void onCreate() {
        this.subscribedAt = LocalDateTime.now();
    }

    public Subscription() {
    }

    public Subscription(User subscriber, Channel channel) {
        this.subscriber = subscriber;
        this.channel = channel;
        this.notificationsEnabled = true;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(User subscriber) {
        this.subscriber = subscriber;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", subscriber=" + (subscriber != null ? subscriber.getUsername() : "null") +
                ", channel=" + (channel != null ? channel.getName() : "null") +
                ", subscribedAt=" + subscribedAt +
                '}';
    }
}