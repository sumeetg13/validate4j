package io.github.sumeetg13.validate4j.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * The rule for one field: which validator to apply, whether the field is mandatory,
 * and an optional message that overrides every message the validator produces.
 *
 * @param field     the field name
 * @param type      the validator type name from YAML (for diagnostics)
 * @param required  whether an absent or blank value is an error
 * @param message   optional message override; may contain {@code {field}}, {@code {value}} and option placeholders
 * @param validator the validator to run when a value is present
 * @param options   the validator's YAML options, used to render placeholders in {@code message}
 */
public record FieldRule(String field, String type, boolean required, String message, Validator validator,
                        Map<String, Object> options) {

    public static final String REQUIRED_CODE = "required";

    public FieldRule {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(validator, "validator");
        options = options == null ? Map.of() : Collections.unmodifiableMap(options);
    }

    public FieldRule(String field, String type, boolean required, Validator validator) {
        this(field, type, required, null, validator, Map.of());
    }
}
