package dev.vaadin.talk.ui;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkType;

/**
 * Create/edit form for a talk, shown as a modal dialog — UC-002 Main Flow
 * step 4, AF-1, AF-4 and AF-5.
 *
 * <p>Every business rule from BR-01 to BR-05 is wired into the {@link Binder},
 * so invalid input is reported per field (BR-07) and the dialog stays open
 * until it is corrected.
 */
public class TalkFormDialog extends Dialog {

    private final Binder<Talk> binder = new Binder<>(Talk.class);

    private final TextField title = new TextField("Title");
    private final TextArea description = new TextArea("Description");
    private final TextField speakerName = new TextField("Speaker");
    private final Select<TalkType> type = new Select<>();
    private final DateTimePicker scheduledDate = new DateTimePicker("Scheduled date");
    private final IntegerField duration = new IntegerField("Duration (minutes)");
    private final TextField location = new TextField("Location");

    private final Consumer<Talk> onSave;
    private final Talk talk;

    /**
     * The date the talk was stored with, or {@code null} for a new talk. BR-04
     * is only re-checked when an edit actually moves the date away from this.
     */
    private final LocalDateTime originalScheduledDate;

    /**
     * Opens a form for a talk.
     *
     * @param talk   the talk to edit, or a fresh instance to create
     * @param onSave called with the populated talk once every rule passes
     */
    public TalkFormDialog(Talk talk, Consumer<Talk> onSave) {
        this.talk = talk;
        this.onSave = onSave;
        this.originalScheduledDate = talk.getScheduledDate();

        boolean editing = talk.getId() != null;
        setHeaderTitle(editing ? "Edit talk" : "Create talk");
        addClassName("talk-form-dialog");
        setDraggable(false);

        configureFields();
        bindFields();
        binder.readBean(talk);

        add(createFormLayout());
        getFooter().add(createCancelButton(), createSaveButton());
    }

    private void configureFields() {
        title.setTestId("form-title");
        title.setRequiredIndicatorVisible(true);

        description.setTestId("form-description");
        description.setRequiredIndicatorVisible(true);
        description.setMaxLength(2000);
        description.setHeight("8em");

        speakerName.setTestId("form-speaker");
        speakerName.setRequiredIndicatorVisible(true);

        type.setLabel("Type");
        type.setItems(TalkType.values());
        type.setItemLabelGenerator(TalkType::getDisplayName);
        type.setPlaceholder("Select a type");
        type.setTestId("form-type");
        type.setRequiredIndicatorVisible(true);

        scheduledDate.setTestId("form-scheduled-date");
        scheduledDate.setRequiredIndicatorVisible(true);
        scheduledDate.setStep(java.time.Duration.ofMinutes(15));

        duration.setTestId("form-duration");
        duration.setRequiredIndicatorVisible(true);
        duration.setStepButtonsVisible(true);
        duration.setSuffixComponent(new com.vaadin.flow.component.html.Span("min"));

        location.setTestId("form-location");
        location.setRequiredIndicatorVisible(true);
    }

    private void bindFields() {
        // BR-01 / BR-02: mandatory, and whitespace alone does not count as text.
        bindRequiredText(title, "Title is required", Talk::getTitle, Talk::setTitle);
        bindRequiredText(description, "Description is required",
                Talk::getDescription, Talk::setDescription);
        bindRequiredText(speakerName, "Speaker name is required",
                Talk::getSpeakerName, Talk::setSpeakerName);
        bindRequiredText(location, "Location is required",
                Talk::getLocation, Talk::setLocation);

        // BR-05: exactly one of the two enum values.
        binder.forField(type)
                .asRequired("Type is required")
                .bind(Talk::getType, Talk::setType);

        // BR-03: a positive number of minutes.
        binder.forField(duration)
                .asRequired("Duration is required")
                .withValidator(value -> value != null && value > 0,
                        "Duration must be a positive number of minutes")
                .bind(Talk::getDuration, Talk::setDuration);

        // BR-04: future date, re-checked only when the edit moves it.
        binder.forField(scheduledDate)
                .asRequired("Scheduled date is required")
                .withValidator(this::isScheduledDateAllowed,
                        "Scheduled date must be in the future")
                .bind(Talk::getScheduledDate, Talk::setScheduledDate);
    }

    /**
     * Binds a mandatory text field, rejecting both an empty value and one that
     * holds nothing but whitespace, and trimming what is stored.
     */
    private <F extends HasValue<?, String> & com.vaadin.flow.component.HasValueAndElement<?, String>>
            void bindRequiredText(F field, String message,
                    ValueProvider<Talk, String> getter, Setter<Talk, String> setter) {
        binder.forField(field)
                .asRequired(message)
                .withValidator(value -> value != null && !value.isBlank(), message)
                // A new talk has null text; a text field cannot hold null.
                .withConverter(String::trim, (String value) -> value == null ? "" : value)
                .bind(getter, setter);
    }

    private boolean isScheduledDateAllowed(LocalDateTime value) {
        if (value == null) {
            return true; // asRequired already reports the missing value.
        }
        if (Objects.equals(value, originalScheduledDate)) {
            return true; // Unchanged on an existing talk — leave it alone.
        }
        return value.isAfter(LocalDateTime.now());
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("32em", 2));
        form.add(title, description, speakerName, type, scheduledDate, duration, location);
        form.setColspan(title, 2);
        form.setColspan(description, 2);
        form.setColspan(location, 2);
        return form;
    }

    private Button createSaveButton() {
        Button save = new Button("Save", event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        save.setTestId("save-talk");
        return save;
    }

    private Button createCancelButton() {
        // AF-5: close without writing anything back to the talk.
        Button cancel = new Button("Cancel", event -> close());
        cancel.addThemeVariants(ButtonVariant.TERTIARY);
        cancel.setTestId("cancel-talk");
        return cancel;
    }

    private void save() {
        try {
            // writeBean validates first and leaves the talk untouched on failure,
            // marking the offending fields so AF-4 shows their messages.
            binder.writeBean(talk);
        } catch (ValidationException e) {
            return;
        }
        onSave.accept(talk);
        close();
    }
}
