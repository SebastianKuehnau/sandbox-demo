package dev.vaadin.talk.domain;

/**
 * Category of a {@link Talk}.
 *
 * <p>See spec {@code datamodel.md} and UC-002 BR-05: a talk is always either a
 * presentation or a workshop.
 */
public enum TalkType {

    PRESENTATION("Presentation"),
    WORKSHOP("Workshop");

    private final String displayName;

    TalkType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable label used in the UI.
     *
     * @return the display name, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }
}
