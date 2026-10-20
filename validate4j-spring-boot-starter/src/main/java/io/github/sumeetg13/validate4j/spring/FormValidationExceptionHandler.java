package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.FormValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts {@link FormValidationException} into a 400 {@code application/problem+json} response:
 *
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Validation failed",
 *   "status": 400,
 *   "detail": "2 field(s) failed validation",
 *   "form": "signup",
 *   "errors": [ { "field": "age", "code": "number.min", "message": "age must be at least 18" } ]
 * }
 * </pre>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class FormValidationExceptionHandler {

    @ExceptionHandler(FormValidationException.class)
    public ProblemDetail handle(FormValidationException ex) {
        List<Map<String, String>> errors = new ArrayList<>(ex.result().errors().size());
        for (FieldError e : ex.result().errors()) {
            errors.add(Map.of("field", e.field(), "code", e.code(), "message", e.message()));
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex.result().failedFields().size() + " field(s) failed validation");
        problem.setTitle("Validation failed");
        problem.setProperty("form", ex.formName());
        problem.setProperty("errors", errors);
        return problem;
    }
}
