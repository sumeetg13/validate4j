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

class StringValidatorTest {

    private static final ValidationContext CTX = new ValidationContext("t", Map.of());

    private static Validator build(Map<String, Object> options) {
        return new StringValidator.Factory().create(ValidatorConfig.of("string", options));
    }

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Test
    void acceptsValidValue() {
        Validator v = build(Map.of("minLength", 2, "maxLength", 5, "pattern", "[a-z]+"));
        assertThat(v.validate("name", "abc", CTX)).isEmpty();
    }

    @Test
    void reportsEachFailingOption() {
        Validator v = build(Map.of("minLength", 2, "maxLength", 5, "pattern", "[a-z]+"));
        assertThat(codes(v.validate("name", "A", CTX))).containsExactly("string.minLength", "string.pattern");
        assertThat(codes(v.validate("name", "abcdefg", CTX))).containsExactly("string.maxLength");
    }

    @Test
    void rendersPlaceholdersInDefaultMessage() {
        Validator v = build(Map.of("minLength", 3));
        assertThat(v.validate("name", "a", CTX).get(0).message()).isEqualTo("name must be at least 3 characters");
    }

    @Test
    void trimsByDefaultAndCanBeDisabled() {
        assertThat(build(Map.of("maxLength", 3)).validate("f", "  abc  ", CTX)).isEmpty();
        assertThat(codes(build(Map.of("maxLength", 3, "trim", false)).validate("f", "  abc  ", CTX)))
                .containsExactly("string.maxLength");
    }

    @Test
    void countsCodePointsNotUtf16Units() {
        assertThat(build(Map.of("maxLength", 2)).validate("f", "😀😀", CTX)).isEmpty();
    }

    @Test
    void rejectsNonText() {
        assertThat(codes(build(Map.of()).validate("f", 42, CTX))).containsExactly("string.type");
    }

    @Test
    void rejectsUnknownOptionAndBadRegexAndInvertedBounds() {
        assertThatThrownBy(() -> build(Map.of("minLenght", 2)))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("minLenght");
        assertThatThrownBy(() -> build(Map.of("pattern", "[")))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("regex");
        assertThatThrownBy(() -> build(Map.of("minLength", 5, "maxLength", 2)))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("must not exceed");
    }
}
