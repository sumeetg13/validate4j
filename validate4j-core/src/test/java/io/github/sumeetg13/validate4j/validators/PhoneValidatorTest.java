package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneValidatorTest {

    private static final ValidationContext CTX = new ValidationContext("t", Map.of());

    private static Validator build(Map<String, Object> options) {
        return new PhoneValidator.Factory().create(ValidatorConfig.of("phone", options));
    }

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Test
    void acceptsAllowedDigitCounts() {
        Validator v = build(Map.of("digits", List.of(10, 12)));
        assertThat(v.validate("phone", "9876543210", CTX)).isEmpty();
        assertThat(v.validate("phone", "919876543210", CTX)).isEmpty();
        assertThat(v.validate("phone", "+91 98765-43210", CTX)).isEmpty();
        assertThat(v.validate("phone", "(987) 654.3210", CTX)).isEmpty();
        assertThat(v.validate("phone", 9876543210L, CTX)).isEmpty();
    }

    @Test
    void scalarDigitsOptionWorks() {
        assertThat(build(Map.of("digits", 10)).validate("phone", "9876543210", CTX)).isEmpty();
    }

    @Test
    void rejectsWrongCountAndLetters() {
        Validator v = build(Map.of("digits", List.of(10, 12)));
        List<FieldError> errors = v.validate("phone", "98765", CTX);
        assertThat(codes(errors)).containsExactly("phone.digits");
        assertThat(errors.get(0).message()).isEqualTo("phone must have 10 or 12 digits");
        assertThat(codes(v.validate("phone", "98765abcde", CTX))).containsExactly("phone.format");
    }

    @Test
    void separatorsAndPlusCanBeDisabled() {
        Validator v = build(Map.of("digits", 10, "allowPlusPrefix", false, "allowSeparators", false));
        assertThat(codes(v.validate("phone", "+9876543210", CTX))).containsExactly("phone.format");
        assertThat(codes(v.validate("phone", "987-654-3210", CTX))).containsExactly("phone.format");
    }

    @Test
    void noDigitsOptionMeansAnyNonEmptyDigitString() {
        Validator v = build(Map.of());
        assertThat(v.validate("phone", "12345", CTX)).isEmpty();
        assertThat(codes(v.validate("phone", "+", CTX))).containsExactly("phone.format");
    }

    @Test
    void rejectsBadConfig() {
        assertThatThrownBy(() -> build(Map.of("digits", List.of(0)))).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> build(Map.of("digits", "ten"))).isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> build(Map.of("length", 10))).isInstanceOf(InvalidConfigurationException.class);
    }
}
