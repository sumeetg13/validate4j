package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumberValidatorTest {

    private static final ValidationContext CTX = new ValidationContext("t", Map.of());

    private static Validator build(Map<String, Object> options) {
        return new NumberValidator.Factory().create(ValidatorConfig.of("number", options));
    }

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Test
    void acceptsNumbersAndNumericStringsInRange() {
        Validator v = build(Map.of("min", 18, "max", 120));
        assertThat(v.validate("age", 18, CTX)).isEmpty();
        assertThat(v.validate("age", 120L, CTX)).isEmpty();
        assertThat(v.validate("age", "42", CTX)).isEmpty();
        assertThat(v.validate("age", new BigDecimal("99.5"), CTX)).isEmpty();
    }

    @Test
    void reportsMinMaxAndInteger() {
        Validator v = build(Map.of("min", 18, "max", 120, "integer", true));
        assertThat(codes(v.validate("age", 17, CTX))).containsExactly("number.min");
        assertThat(codes(v.validate("age", 121, CTX))).containsExactly("number.max");
        assertThat(codes(v.validate("age", 20.5, CTX))).containsExactly("number.integer");
        assertThat(v.validate("age", 20.0, CTX)).isEmpty();
    }

    @Test
    void rejectsNonNumeric() {
        assertThat(codes(build(Map.of()).validate("age", "abc", CTX))).containsExactly("number.type");
        assertThat(codes(build(Map.of()).validate("age", List.of(), CTX))).containsExactly("number.type");
        assertThat(codes(build(Map.of()).validate("age", Double.NaN, CTX))).containsExactly("number.type");
    }

    @Test
    void messageUsesPlainNumbers() {
        assertThat(build(Map.of("min", 18)).validate("age", 1, CTX).get(0).message()).isEqualTo("age must be at least 18");
    }

    @Test
    void rejectsBadConfig() {
        assertThatThrownBy(() -> build(Map.of("min", "abc"))).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> build(Map.of("min", 5, "max", 1))).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> build(Map.of("minimum", 5))).isInstanceOf(InvalidConfigurationException.class);
    }
}
