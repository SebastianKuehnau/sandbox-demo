package dev.vaadin.talk.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkRepository;
import dev.vaadin.talk.domain.TalkType;

/**
 * Fills an empty database with a demo catalog so the public listing is not
 * blank on a first run.
 *
 * <p>Runs only when the {@code talk} table has no rows, so administrator edits
 * survive restarts of the file-based H2 database. Excluded from the
 * {@code test} profile: tests set up exactly the data they assert on.
 */
@Component
@Profile("!test")
public class TalkDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TalkDataSeeder.class);

    private final TalkRepository repository;

    public TalkDataSeeder(TalkRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        List<Talk> demoTalks = createDemoTalks();
        repository.saveAll(demoTalks);
        log.info("Seeded empty database with {} demo talks", demoTalks.size());
    }

    /**
     * Builds the demo catalog. All dates are relative to today so the seeded
     * talks always satisfy UC-002 BR-04, no matter when the app first starts.
     */
    private List<Talk> createDemoTalks() {
        LocalDate today = LocalDate.now();
        return List.of(
                new Talk("Signals in Vaadin 25",
                        "A deep dive into the new reactive Signals API: how element-level binding "
                                + "works, when to reach for shared signals, and what changes in your "
                                + "state management once they replace manual UI updates.",
                        "Amara Osei", TalkType.PRESENTATION,
                        at(today, 7, 9, 0), 45, "Main Hall"),
                new Talk("Building Design Systems with Aura",
                        "Aura replaces Lumo as the default theme in Vaadin 25. Learn how its "
                                + "computed colour, spacing and typography tokens work, and how to "
                                + "shape an entire product identity from a handful of base properties.",
                        "Jonas Lindqvist", TalkType.PRESENTATION,
                        at(today, 7, 11, 0), 30, "Main Hall"),
                new Talk("Hands-on: Your First Full-Stack Vaadin App",
                        "Bring a laptop. Over three hours we build a working application from an "
                                + "empty directory: entities, a Spring Data repository, a Grid-based "
                                + "admin view, and a public view with filtering.",
                        "Priya Raghunathan", TalkType.WORKSHOP,
                        at(today, 8, 9, 30), 180, "Workshop Room A"),
                new Talk("Testing Vaadin Views Without a Browser",
                        "Browserless tests run your Flow views on the JVM in milliseconds. We cover "
                                + "the component query API, how to structure tests around use cases "
                                + "instead of classes, and where a real browser is still worth it.",
                        "Marcus Feldt", TalkType.PRESENTATION,
                        at(today, 8, 13, 0), 45, "Room 204"),
                new Talk("Accessibility Workshop: WCAG for Business Apps",
                        "A practical session on keyboard navigation, focus management, colour "
                                + "contrast and screen-reader labelling. We audit a real application "
                                + "together and fix what we find.",
                        "Lena Hartmann", TalkType.WORKSHOP,
                        at(today, 9, 10, 0), 120, "Workshop Room B"),
                new Talk("Spring Boot 4 in Production",
                        "What actually changed in Spring Boot 4 and Spring Framework 7, which "
                                + "migration steps bite hardest, and how to plan an upgrade that does "
                                + "not stall halfway through.",
                        "Diego Ferreira", TalkType.PRESENTATION,
                        at(today, 9, 14, 0), 60, "Main Hall"),
                new Talk("Data Grids That Scale to Millions of Rows",
                        "Lazy loading, server-side sorting and filtering, and the memory profile of "
                                + "a Vaadin Grid under load. Includes benchmarks and the mistakes that "
                                + "most often cause slow grids.",
                        "Sofia Almeida", TalkType.PRESENTATION,
                        at(today, 10, 9, 0), 45, "Room 204"),
                new Talk("Workshop: Theming a Vaadin App End to End",
                        "Take a default-styled application and give it a distinct visual identity. "
                                + "We work through tokens, component parts, dark mode and responsive "
                                + "breakpoints, and finish with a themed app you can take home.",
                        "Tobias Krenn", TalkType.WORKSHOP,
                        at(today, 10, 13, 30), 150, "Workshop Room A"));
    }

    private static LocalDateTime at(LocalDate today, int daysFromNow, int hour, int minute) {
        return today.plusDays(daysFromNow).atTime(hour, minute);
    }
}
