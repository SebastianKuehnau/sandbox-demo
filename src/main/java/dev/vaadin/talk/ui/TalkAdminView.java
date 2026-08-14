package dev.vaadin.talk.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.service.TalkService;

/**
 * Administration of the talk catalog — UC-002.
 *
 * <p>Lists every talk in a grid with per-row edit and delete actions, and
 * offers a create button above it. Deletions go through a confirmation dialog
 * (BR-06) and every successful write reports back with a notification.
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Manage Talks")
public class TalkAdminView extends Main {

    /** Column key for the actions column, used to reach its buttons in tests. */
    public static final String ACTIONS_COLUMN_KEY = "actions";

    private final TalkService talkService;
    private final Grid<Talk> grid = new Grid<>(Talk.class, false);

    public TalkAdminView(TalkService talkService) {
        this.talkService = talkService;
        addClassName("talk-admin-view");

        add(createHeader(), createGrid());
        refresh();
    }

    private Div createHeader() {
        H1 heading = new H1("Manage talks");
        Paragraph subtitle = new Paragraph(
                "Create, edit and remove presentations and workshops.");
        subtitle.addClassName("view-subtitle");

        Button createButton = new Button("Create talk", VaadinIcon.PLUS.create(),
                event -> openForm(new Talk()));
        createButton.addThemeVariants(ButtonVariant.PRIMARY);
        createButton.setTestId("create-talk");

        Div titleBlock = new Div(heading, subtitle);
        Div header = new Div(titleBlock, createButton);
        header.addClassNames("view-header", "admin-header");
        return header;
    }

    private Grid<Talk> createGrid() {
        grid.addColumn(Talk::getTitle).setHeader("Title").setKey("title")
                .setSortable(true).setFlexGrow(3);
        grid.addColumn(Talk::getSpeakerName).setHeader("Speaker").setKey("speaker")
                .setSortable(true).setFlexGrow(2);
        grid.addColumn(talk -> talk.getType().getDisplayName()).setHeader("Type").setKey("type")
                .setSortable(true).setFlexGrow(1);
        grid.addColumn(talk -> TalkFormats.formatDateTime(talk.getScheduledDate()))
                .setHeader("Scheduled").setKey("scheduledDate").setSortable(true).setFlexGrow(2);
        grid.addColumn(talk -> TalkFormats.formatDuration(talk.getDuration()))
                .setHeader("Duration").setKey("duration").setFlexGrow(1);
        grid.addComponentColumn(this::createRowActions).setHeader("Actions")
                .setKey(ACTIONS_COLUMN_KEY).setFlexGrow(0).setAutoWidth(true);

        grid.setTestId("talk-grid");
        grid.addClassName("talk-grid");
        return grid;
    }

    private Div createRowActions(Talk talk) {
        Button edit = new Button(VaadinIcon.EDIT.create(), event -> openForm(talk));
        edit.addThemeVariants(ButtonVariant.TERTIARY);
        edit.setAriaLabel("Edit " + talk.getTitle());
        edit.setTooltipText("Edit");

        Button delete = new Button(VaadinIcon.TRASH.create(), event -> confirmDelete(talk));
        delete.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        delete.setAriaLabel("Delete " + talk.getTitle());
        delete.setTooltipText("Delete");

        Div actions = new Div(edit, delete);
        actions.addClassName("row-actions");
        return actions;
    }

    private void openForm(Talk talk) {
        boolean creating = talk.getId() == null;
        new TalkFormDialog(talk, saved -> {
            talkService.save(saved);
            refresh();
            showSuccess(creating ? "Talk created" : "Talk updated");
        }).open();
    }

    /** AF-2 / AF-3 and BR-06: never delete without an explicit confirmation. */
    private void confirmDelete(Talk talk) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete talk?");
        dialog.setText("Are you sure you want to delete this talk?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            talkService.delete(talk.getId());
            refresh();
            showSuccess("Talk deleted");
        });
        dialog.open();
    }

    private void refresh() {
        grid.setItems(talkService.findAll());
    }

    private static void showSuccess(String message) {
        Notification notification =
                Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.SUCCESS);
    }
}
