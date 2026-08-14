package dev.vaadin.usecases.playwright;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkRepository;

/**
 * Shared setup for the Playwright end-to-end tests.
 *
 * <p>These cover the same use cases as the browserless tests, but through a real
 * browser against a real server: the browserless tests exercise the Flow view on
 * the JVM, these prove the same behaviour survives rendering, the client-server
 * round trip and the web components.
 *
 * <p>The application runs in the test JVM on a random port, so no external server
 * has to be started. The {@code test} profile keeps the database in memory and
 * switches the demo-data seeder off, so each test owns its data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractPlaywrightE2ETest {

    /** Matches the resolution used for visual verification. */
    private static final int VIEWPORT_WIDTH = 1920;
    private static final int VIEWPORT_HEIGHT = 1080;

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    @Autowired
    protected TalkRepository repository;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                // Pinned so the suite does not inherit the developer's locale.
                .setLocale("en-US"));
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    /** Navigates to an application route and waits for Vaadin to finish rendering. */
    protected void open(String path) {
        page.navigate("http://localhost:" + port + path);
        waitForVaadin();
    }

    /**
     * Waits until Vaadin reports no request in flight, so assertions do not race
     * an in-progress server round trip.
     */
    protected void waitForVaadin() {
        page.waitForFunction("() => {"
                + "  const c = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients;"
                + "  if (!c) return false;"
                + "  return Object.keys(c).filter(k => k !== 'TypeScript')"
                + "    .every(k => c[k].isActive && !c[k].isActive());"
                + "}");
    }

    /** Replaces the catalog with exactly the given talks. */
    protected void givenTalks(List<Talk> talks) {
        repository.deleteAll();
        repository.saveAll(talks);
    }

    /** A scheduled date safely in the future, with seconds/nanos stripped for stable formatting. */
    protected static LocalDateTime futureDate(int daysFromNow) {
        return LocalDateTime.now().plusDays(daysFromNow).withSecond(0).withNano(0);
    }

    /**
     * The talk cards the visitor can actually see.
     *
     * <p>The {@code :visible} filter is essential rather than cosmetic. When the
     * result list becomes empty, Flow hides its container instead of clearing the
     * client DOM — hidden elements are not updated client-side — so the previous
     * cards stay in the document at zero height. Counting raw nodes would report
     * them as still listed.
     */
    protected Locator talkCards() {
        return page.locator("vaadin-card.talk-card:visible");
    }

    /**
     * The titles of the rendered talk cards, in display order. The card renders its
     * title into its own shadow root, but also carries it as the accessible name,
     * which is stable to read.
     */
    @SuppressWarnings("unchecked")
    protected List<String> visibleCardTitles() {
        return (List<String>) talkCards()
                .evaluateAll("nodes => nodes.map(n => n.getAttribute('aria-label'))");
    }
}
