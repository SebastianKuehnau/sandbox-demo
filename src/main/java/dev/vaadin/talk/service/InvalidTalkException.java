package dev.vaadin.talk.service;

/**
 * Thrown when a talk violates a business rule that Bean Validation cannot
 * express on the entity alone — currently only UC-002 BR-04, which depends on
 * the current time and on whether the talk is new or edited.
 */
public class InvalidTalkException extends RuntimeException {

    public InvalidTalkException(String message) {
        super(message);
    }
}
