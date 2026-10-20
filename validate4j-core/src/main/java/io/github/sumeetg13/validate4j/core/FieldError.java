package io.github.sumeetg13.validate4j.core;

import java.util.Objects;

/**
 * One validation failure.
 *
 * @param field   the field name
 * @param code    a stable machine-readable code such as {@code string.minLength}
 * @param message a human-readable message with placeholders already rendered
 */
public record FieldError(String field, String code, String message) {

    public FieldError {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    public FieldError withMessage(String newMessage) {
        return new FieldError(field, code, newMessage);
    }
}
