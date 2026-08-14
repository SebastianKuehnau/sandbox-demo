package dev.vaadin.talk.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.vaadin.talk.domain.Talk;
import dev.vaadin.talk.domain.TalkRepository;
import dev.vaadin.talk.domain.TalkType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Business logic for the talk catalog.
 *
 * <p>Covers the querying behaviour of UC-001 (search and type filtering) and
 * the write behaviour and business rules of UC-002.
 */
@Service
@Transactional
public class TalkService {

    private static final Sort BY_DATE = Sort.by(Sort.Direction.ASC, "scheduledDate");

    private final TalkRepository repository;
    private final Validator validator;

    public TalkService(TalkRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Returns every talk, earliest first.
     *
     * @return all talks, never {@code null}
     */
    @Transactional(readOnly = true)
    public List<Talk> findAll() {
        return repository.findAll(BY_DATE);
    }

    /**
     * Finds talks matching an optional free-text search and an optional type.
     *
     * <p>Implements UC-001 BR-02: the search is case-insensitive and matches
     * against title, description and speaker name. Passing {@code null} or
     * blank text disables the text filter; passing a {@code null} type
     * corresponds to the "ALL" option of BR-03.
     *
     * @param searchText free-text to look for, may be {@code null} or blank
     * @param type       the type to restrict to, or {@code null} for all types
     * @return the matching talks, earliest first, never {@code null}
     */
    @Transactional(readOnly = true)
    public List<Talk> search(String searchText, TalkType type) {
        return repository.findAll(matching(searchText, type), BY_DATE);
    }

    /**
     * Finds a single talk by its id.
     *
     * @param id the talk id
     * @return the talk, or empty if it does not exist
     */
    @Transactional(readOnly = true)
    public Optional<Talk> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Creates or updates a talk after checking every UC-002 business rule.
     *
     * <p>BR-01 through BR-03 and BR-05 come from the Bean Validation
     * constraints on {@link Talk}. BR-04 is enforced here: a new talk must be
     * scheduled in the future, and an edit must do the same whenever it
     * actually moves the date — leaving a past date untouched stays allowed so
     * that talks which have already happened remain editable.
     *
     * @param talk the talk to persist
     * @return the persisted talk, with its id assigned
     * @throws ConstraintViolationException if a field-level constraint fails
     * @throws InvalidTalkException         if the scheduled date violates BR-04
     */
    public Talk save(Talk talk) {
        Objects.requireNonNull(talk, "talk must not be null");
        validateFields(talk);
        validateScheduledDate(talk);
        return repository.save(talk);
    }

    /**
     * Deletes a talk.
     *
     * @param id the id of the talk to delete
     */
    public void delete(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        repository.deleteById(id);
    }

    /**
     * Returns the number of talks in the catalog.
     *
     * @return the talk count
     */
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    private void validateFields(Talk talk) {
        Set<ConstraintViolation<Talk>> violations = validator.validate(talk);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateScheduledDate(Talk talk) {
        LocalDateTime scheduledDate = talk.getScheduledDate();
        boolean dateNeedsToBeInFuture;
        if (talk.getId() == null) {
            dateNeedsToBeInFuture = true;
        } else {
            // Only re-check the rule when the edit actually moved the date.
            dateNeedsToBeInFuture = repository.findScheduledDateById(talk.getId())
                    .map(stored -> !stored.equals(scheduledDate))
                    .orElse(true);
        }
        if (dateNeedsToBeInFuture && !scheduledDate.isAfter(LocalDateTime.now())) {
            throw new InvalidTalkException("Scheduled date must be in the future");
        }
    }

    private Specification<Talk> matching(String searchText, TalkType type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (searchText != null && !searchText.isBlank()) {
                String pattern = "%" + searchText.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("speakerName")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
