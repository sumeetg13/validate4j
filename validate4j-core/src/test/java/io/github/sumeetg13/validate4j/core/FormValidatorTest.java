package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.validators.NumberValidator;
import io.github.sumeetg13.validate4j.validators.StringValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormValidatorTest {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);

    @AfterAll
    static void shutdown() {
        POOL.shutdownNow();
    }

    private static FieldRule stringRule(String field, boolean required, String message) {
        return new FieldRule(field, "string", required, message,
                new StringValidator.Factory().create(ValidatorConfig.of("string", Map.of("minLength", 2))),
                Map.of("minLength", 2));
    }

    private static FieldRule numberRule(String field) {
        return new FieldRule(field, "number", false, null,
                new NumberValidator.Factory().create(ValidatorConfig.of("number", Map.of("min", 18))),
                Map.of("min", 18));
    }

    @Test
    void requiredFieldReportsAbsentOrBlank() {
        FormValidator v = new FormValidator(new FormDefinition("f", List.of(stringRule("name", true, null))));
        assertThat(v.validate(Map.of()).errors()).extracting(FieldError::code).containsExactly("required");
        Map<String, Object> blank = new HashMap<>();
        blank.put("name", "   ");
        assertThat(v.validate(blank).errors()).extracting(FieldError::code).containsExactly("required");
        Map<String, Object> nul = new HashMap<>();
        nul.put("name", null);
        assertThat(v.validate(nul).errors()).extracting(FieldError::code).containsExactly("required");
    }

    @Test
    void optionalFieldSkipsValidationWhenAbsent() {
        FormValidator v = new FormValidator(new FormDefinition("f", List.of(stringRule("nick", false, null))));
        assertThat(v.validate(Map.of()).isValid()).isTrue();
        assertThat(v.validate(Map.of("nick", "x")).isValid()).isFalse();
    }

    @Test
    void messageOverrideAppliesToEveryErrorWithPlaceholders() {
        FormValidator v = new FormValidator(new FormDefinition("f",
                List.of(stringRule("name", true, "{field} needs {minLength}+ chars, got '{value}'"))));
        List<FieldError> errors = v.validate(Map.of("name", "x")).errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).code()).isEqualTo("string.minLength");
        assertThat(errors.get(0).message()).isEqualTo("name needs 2+ chars, got 'x'");
        assertThat(v.validate(Map.of()).errors().get(0).message()).isEqualTo("name needs 2+ chars, got 'null'");
    }

    @Test
    void parallelResultsKeepDeclarationOrder() {
        List<FieldRule> rules = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            rules.add(stringRule("f" + i, true, null));
        }
        FormValidator parallel = new FormValidator(new FormDefinition("big", rules), POOL, 1);
        FormValidator sequential = new FormValidator(new FormDefinition("big", rules));
        Map<String, Object> input = Map.of("f3", "ok", "f7", "x");
        ValidationResult p = parallel.validate(input);
        ValidationResult s = sequential.validate(input);
        assertThat(p.errors()).isEqualTo(s.errors());
        assertThat(p.failedFields()).hasSize(49).doesNotContain("f3");
        assertThat(p.errorsFor("f7")).singleElement().extracting(FieldError::code).isEqualTo("string.minLength");
        List<String> order = p.errors().stream().map(FieldError::field).toList();
        List<String> expected = rules.stream().map(FieldRule::field).filter(f -> !f.equals("f3")).toList();
        assertThat(order).isEqualTo(expected);
    }

    @Test
    void thresholdDecidesParallelism() {
        Set<String> threads = ConcurrentHashMap.newKeySet();
        Validator spy = (field, value, ctx) -> {
            threads.add(Thread.currentThread().getName());
            return List.of();
        };
        List<FieldRule> rules = List.of(
                new FieldRule("a", "spy", true, spy), new FieldRule("b", "spy", true, spy), new FieldRule("c", "spy", true, spy));
        Map<String, Object> input = Map.of("a", 1, "b", 2, "c", 3);

        new FormValidator(new FormDefinition("f", rules), POOL, 10).validate(input);
        assertThat(threads).containsExactly(Thread.currentThread().getName());

        threads.clear();
        new FormValidator(new FormDefinition("f", rules), POOL, 3).validate(input);
        assertThat(threads).doesNotContain(Thread.currentThread().getName());
    }

    @Test
    void validatorExceptionsPropagateFromParallelRun() {
        Validator boom = (field, value, ctx) -> {
            throw new IllegalStateException("boom");
        };
        FormValidator v = new FormValidator(new FormDefinition("f", List.of(
                new FieldRule("a", "x", true, boom), new FieldRule("b", "x", true, boom))), POOL, 1);
        assertThatThrownBy(() -> v.validate(Map.of("a", 1, "b", 2)))
                .isInstanceOf(IllegalStateException.class).hasMessage("boom");
    }

    @Test
    void validateOrThrowCarriesResult() {
        FormValidator v = new FormValidator(new FormDefinition("f", List.of(stringRule("name", true, null), numberRule("age"))));
        assertThatThrownBy(() -> v.validateOrThrow(Map.of("age", 5)))
                .isInstanceOf(FormValidationException.class)
                .satisfies(e -> {
                    FormValidationException fve = (FormValidationException) e;
                    assertThat(fve.formName()).isEqualTo("f");
                    assertThat(fve.result().failedFields()).containsExactly("name", "age");
                });
        assertThat(v.validateOrThrow(Map.of("name", "Al", "age", 30)).isValid()).isTrue();
    }

    @Test
    void duplicateFieldsAreRejected() {
        assertThatThrownBy(() -> new FormDefinition("f", List.of(stringRule("a", true, null), stringRule("a", true, null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate field 'a'");
    }
}
