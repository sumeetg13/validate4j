package io.github.sumeetg13.validate4j.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Read-only view of the full submission, passed to every validator. */
public final class ValidationContext {

    private final String formName;
    private final Map<String, Object> input;

    public ValidationContext(String formName, Map<String, Object> input) {
        this.formName = Objects.requireNonNull(formName, "formName");
        this.input = Collections.unmodifiableMap(Objects.requireNonNull(input, "input"));
    }

    public String formName() {
        return formName;
    }

    /** The complete, unmodifiable input map. */
    public Map<String, Object> input() {
        return input;
    }

    public Object value(String field) {
        return input.get(field);
    }
}
