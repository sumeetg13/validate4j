package io.github.sumeetg13.validate4j.core;

import java.util.Objects;

/** Thrown by {@link FormValidator#validateOrThrow} when a submission has errors. */
public class FormValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String formName;
    private final transient ValidationResult result;

    public FormValidationException(String formName, ValidationResult result) {
        super("Validation failed for form '" + formName + "': " + result.errors());
        this.formName = Objects.requireNonNull(formName, "formName");
        this.result = Objects.requireNonNull(result, "result");
    }

    public String formName() {
        return formName;
    }

    public ValidationResult result() {
        return result;
    }
}
