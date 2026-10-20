package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Validates submissions against one {@link FormDefinition}.
 *
 * <p>Fields are independent, so when a form has at least {@code parallelThreshold} fields each
 * field is validated as its own task on the supplied {@link Executor}. Results are merged in
 * declaration order, so the output is deterministic regardless of scheduling. Smaller forms run
 * sequentially on the calling thread because the hand-off cost outweighs a few regex checks.
 */
public final class FormValidator {

    /** Default number of fields at which parallel execution kicks in. */
    public static final int DEFAULT_PARALLEL_THRESHOLD = 8;

    private final FormDefinition definition;
    private final Executor executor;
    private final int parallelThreshold;

    /**
     * @param definition        the form
     * @param executor          executor for parallel validation; {@code null} forces sequential mode
     * @param parallelThreshold minimum field count for parallel validation (values below 1 mean always parallel)
     */
    public FormValidator(FormDefinition definition, Executor executor, int parallelThreshold) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.executor = executor;
        this.parallelThreshold = parallelThreshold;
    }

    /** Sequential validator, useful for tests and tiny forms. */
    public FormValidator(FormDefinition definition) {
        this(definition, null, Integer.MAX_VALUE);
    }

    public FormDefinition definition() {
        return definition;
    }

    public String name() {
        return definition.name();
    }

    public ValidationResult validate(Map<String, Object> input) {
        Objects.requireNonNull(input, "input");
        ValidationContext context = new ValidationContext(definition.name(), input);
        List<FieldRule> rules = definition.fields();
        boolean parallel = executor != null && rules.size() >= parallelThreshold && rules.size() > 1;
        return parallel ? validateParallel(rules, context) : validateSequential(rules, context);
    }

    /** Like {@link #validate} but throws {@link FormValidationException} when there are errors. */
    public ValidationResult validateOrThrow(Map<String, Object> input) {
        ValidationResult result = validate(input);
        if (result.hasErrors()) {
            throw new FormValidationException(definition.name(), result);
        }
        return result;
    }

    private ValidationResult validateSequential(List<FieldRule> rules, ValidationContext context) {
        List<FieldError> errors = new ArrayList<>();
        for (FieldRule rule : rules) {
            errors.addAll(validateField(rule, context));
        }
        return ValidationResult.of(errors);
    }

    private ValidationResult validateParallel(List<FieldRule> rules, ValidationContext context) {
        List<CompletableFuture<List<FieldError>>> futures = new ArrayList<>(rules.size());
        for (FieldRule rule : rules) {
            futures.add(CompletableFuture.supplyAsync(() -> validateField(rule, context), executor));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Validation task failed", cause);
        }
        List<FieldError> errors = new ArrayList<>();
        for (CompletableFuture<List<FieldError>> future : futures) {
            errors.addAll(future.join());
        }
        return ValidationResult.of(errors);
    }

    private List<FieldError> validateField(FieldRule rule, ValidationContext context) {
        Object value = context.value(rule.field());
        List<FieldError> errors;
        if (isAbsent(value) && !rule.validator().acceptsNull()) {
            errors = rule.required()
                    ? List.of(new FieldError(rule.field(), FieldRule.REQUIRED_CODE, rule.field() + " is required"))
                    : List.of();
        } else {
            errors = rule.validator().validate(rule.field(), value, context);
        }
        if (errors.isEmpty() || rule.message() == null) {
            return errors;
        }
        Map<String, Object> vars = new HashMap<>(rule.options());
        vars.put("field", rule.field());
        vars.put("value", value);
        String rendered = MessageTemplate.render(rule.message(), vars);
        List<FieldError> overridden = new ArrayList<>(errors.size());
        for (FieldError error : errors) {
            overridden.add(error.withMessage(rendered));
        }
        return overridden;
    }

    private static boolean isAbsent(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence cs) {
            return cs.toString().isBlank();
        }
        return false;
    }
}
