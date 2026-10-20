package io.github.sumeetg13.validate4j.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a {@code @RequestBody} parameter against the named YAML form before the handler runs.
 * A failed validation raises {@link io.github.sumeetg13.validate4j.core.FormValidationException}, which the
 * starter turns into an RFC 9457 {@code ProblemDetail} with status 400.
 *
 * <pre>{@code
 * @PostMapping("/signup")
 * public Map<String, Object> signup(@ValidateWith("signup") @RequestBody Map<String, Object> body) { ... }
 * }</pre>
 *
 * The body may be a {@code Map}, a record, or a JavaBean; see {@link FormInputConverter}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ValidateWith {

    /** The form name as declared in YAML. */
    String value();
}
