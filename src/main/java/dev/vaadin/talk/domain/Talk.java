package dev.vaadin.talk.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A presentation or workshop in the talk catalog.
 *
 * <p>Field set and mandatory-ness follow spec {@code datamodel.md} and UC-002
 * BR-01 through BR-05: every field is required, the duration is a positive
 * number of minutes, and the type is one of {@link TalkType}.
 */
@Entity
@Table(name = "talk")
public class Talk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 2000)
    private String description;

    @NotBlank(message = "Speaker name is required")
    @Column(name = "speaker_name", nullable = false)
    private String speakerName;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TalkType type;

    @NotNull(message = "Scheduled date is required")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    /** Length of the talk in minutes. Mapped explicitly to avoid SQL keyword clashes. */
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number of minutes")
    @Column(name = "duration_minutes", nullable = false)
    private Integer duration;

    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;

    /** Required by JPA. */
    public Talk() {
    }

    public Talk(String title, String description, String speakerName, TalkType type,
            LocalDateTime scheduledDate, Integer duration, String location) {
        this.title = title;
        this.description = description;
        this.speakerName = speakerName;
        this.type = type;
        this.scheduledDate = scheduledDate;
        this.duration = duration;
        this.location = location;
    }

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

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public TalkType getType() {
        return type;
    }

    public void setType(TalkType type) {
        this.type = type;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Talk talk)) {
            return false;
        }
        // Unsaved entities are only ever equal to themselves.
        return id != null && id.equals(talk.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Talk[id=%s, title=%s]".formatted(id, title);
    }
}
