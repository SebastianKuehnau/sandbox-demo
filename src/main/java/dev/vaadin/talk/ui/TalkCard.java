package dev.vaadin.talk.ui;

import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkType;

/**
 * One talk rendered as a card in the public listing.
 *
 * <p>Shows everything UC-001 BR-01 requires — title, speaker, type and
 * scheduled date — plus the duration and location, so a visitor can judge a
 * talk without opening anything.
 */
public class TalkCard extends Card implements HasStyle {

    private final Talk talk;

    public TalkCard(Talk talk) {
        this.talk = talk;
        addClassName("talk-card");
        addThemeVariants(CardVariant.OUTLINED);
        setAriaLabel(talk.getTitle());

        setTitle(talk.getTitle());
        setSubtitle(talk.getSpeakerName());
        setHeaderSuffix(createTypeBadge(talk.getType()));

        Paragraph description = new Paragraph(talk.getDescription());
        description.addClassName("talk-card-description");
        add(description);

        Div meta = new Div(
                metaItem(VaadinIcon.CALENDAR, TalkFormats.formatDateTime(talk.getScheduledDate())),
                metaItem(VaadinIcon.CLOCK, TalkFormats.formatDuration(talk.getDuration())),
                metaItem(VaadinIcon.MAP_MARKER, talk.getLocation()));
        meta.addClassName("talk-card-meta");
        addToFooter(meta);
    }

    /**
     * Returns the talk this card renders.
     *
     * @return the talk, never {@code null}
     */
    public Talk getTalk() {
        return talk;
    }

    private static Badge createTypeBadge(TalkType type) {
        Badge badge = new Badge(type.getDisplayName());
        // Two categories, two visually distinct badges.
        badge.addThemeVariants(
                type == TalkType.WORKSHOP ? BadgeVariant.SUCCESS : BadgeVariant.CONTRAST);
        return badge;
    }

    private static Span metaItem(VaadinIcon icon, String text) {
        Icon iconComponent = icon.create();
        iconComponent.addClassName("talk-card-meta-icon");
        Span item = new Span(iconComponent, new Span(text));
        item.addClassName("talk-card-meta-item");
        return item;
    }
}
