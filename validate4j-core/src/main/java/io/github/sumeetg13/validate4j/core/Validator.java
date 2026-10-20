package io.github.sumeetg13.validate4j.core;

import java.util.List;

/**
 * Validates a single field value. Implementations must be immutable and thread-safe:
 * a validator is built once from its YAML configuration and may be invoked concurrently.
 *
 * <p>A validator never throws for an invalid value. It returns zero or more {@link FieldError}s.
 * The engine handles {@code null}/absent values before calling a validator, so implementations
 * can assume {@code value} is non-null unless they are registered with
 * {@link #acceptsNull()} returning {@code true}.
 */
@FunctionalInterface
public interface Validator {

    /**
     * @param field   the field name as declared in YAML
     * @param value   the submitted value (never {@code null} unless {@link #acceptsNull()} is true)
     * @param context the whole submission, for validators that need it
     * @return the list of errors for this field; empty when valid
     */
    List<FieldError> validate(String field, Object value, ValidationContext context);

    /** Whether this validator wants to see {@code null} values. Default: no. */
    default boolean acceptsNull() {
        return false;
    }
}
