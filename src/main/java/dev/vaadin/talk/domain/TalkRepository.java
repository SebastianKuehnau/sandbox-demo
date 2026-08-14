package dev.vaadin.talk.domain;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Talk}.
 *
 * <p>Filtering for UC-001 is expressed with {@link JpaSpecificationExecutor}
 * rather than a static query, because the search text and the type filter are
 * independently optional.
 */
public interface TalkRepository extends JpaRepository<Talk, Long>, JpaSpecificationExecutor<Talk> {

    /**
     * Reads just the stored scheduled date of a talk.
     *
     * <p>Used by the UC-002 BR-04 check to tell whether an edit actually moved
     * the date. Returns a scalar so the caller does not pull a managed entity
     * into the persistence context alongside the detached one being saved.
     *
     * @param id the talk id
     * @return the persisted scheduled date, or empty if no such talk exists
     */
    @Query("SELECT t.scheduledDate FROM Talk t WHERE t.id = :id")
    Optional<LocalDateTime> findScheduledDateById(@Param("id") Long id);
}
