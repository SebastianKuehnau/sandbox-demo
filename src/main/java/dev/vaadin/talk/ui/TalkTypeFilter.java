package dev.vaadin.talk.ui;

import dev.vaadin.talk.domain.TalkType;

/**
 * The options offered by the talk type filter on the public listing.
 *
 * <p>Implements UC-001 BR-03: the filter offers PRESENTATION, WORKSHOP and ALL.
 * {@link #ALL} maps to a {@code null} {@link TalkType}, which the service reads
 * as "do not filter by type".
 */
public enum TalkTypeFilter {

    ALL("All", null),
    PRESENTATION("Presentations", TalkType.PRESENTATION),
    WORKSHOP("Workshops", TalkType.WORKSHOP);

    private final String displayName;
    private final TalkType talkType;

    TalkTypeFilter(String displayName, TalkType talkType) {
        this.displayName = displayName;
        this.talkType = talkType;
    }

    /**
     * Returns the label shown in the filter control.
     *
     * @return the display name, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the talk type this option restricts to.
     *
     * @return the talk type, or {@code null} for {@link #ALL}
     */
    public TalkType getTalkType() {
        return talkType;
    }
}
