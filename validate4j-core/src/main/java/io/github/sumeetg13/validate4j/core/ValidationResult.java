package io.github.sumeetg13.validate4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable outcome of validating one form submission. */
public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(List.of());

    private final List<FieldError> errors;

    private ValidationResult(List<FieldError> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static ValidationResult valid() {
        return VALID;
    }

    public static ValidationResult of(List<FieldError> errors) {
        return errors.isEmpty() ? VALID : new ValidationResult(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** All errors, in field declaration order. */
    public List<FieldError> errors() {
        return errors;
    }

    public List<FieldError> errorsFor(String field) {
        return errors.stream().filter(e -> e.field().equals(field)).collect(Collectors.toUnmodifiableList());
    }

    /** Names of the fields that failed, in declaration order. */
    public Set<String> failedFields() {
        Set<String> fields = new LinkedHashSet<>();
        for (FieldError e : errors) {
            fields.add(e.field());
        }
        return Collections.unmodifiableSet(fields);
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult[valid]" : "ValidationResult" + errors;
    }
}
