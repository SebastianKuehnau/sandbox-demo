package dev.vaadin.talk.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLink;

/**
 * Application shell shared by the public listing and the admin view.
 *
 * <p>The two views are separate routes with no other way to reach each other,
 * so the navbar carries a link to each and highlights the active one.
 */
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final Tabs navigation = new Tabs();
    private final Map<Class<? extends Component>, Tab> tabsByView = new LinkedHashMap<>();

    public MainLayout() {
        addClassName("main-layout");

        Span appName = new Span("TalkHub");
        appName.addClassName("app-name");

        addNavigationTab("Talks", TalkListView.class);
        addNavigationTab("Manage talks", TalkAdminView.class);
        navigation.addClassName("main-nav");

        Header header = new Header(appName, navigation);
        header.addClassName("app-header");
        addToNavbar(header);
    }

    private void addNavigationTab(String label, Class<? extends Component> target) {
        Tab tab = new Tab(new RouterLink(label, target));
        tabsByView.put(target, tab);
        navigation.add(tab);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        event.getActiveChain().stream()
                .map(Object::getClass)
                .map(tabsByView::get)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .ifPresent(navigation::setSelectedTab);
    }
}
