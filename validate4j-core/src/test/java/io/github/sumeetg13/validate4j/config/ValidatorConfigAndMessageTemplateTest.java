package io.github.sumeetg13.validate4j.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorConfigAndMessageTemplateTest {

    @Test
    void typedAccessorsCoerceScalars() {
        ValidatorConfig c = ValidatorConfig.of("t", Map.of("i", "7", "b", "true", "n", 1.5, "l", List.of(1, "2")));
        assertThat(c.requireInt("i")).isEqualTo(7);
        assertThat(c.booleanOr("b", false)).isTrue();
        assertThat(c.optionalNumber("n")).contains(new BigDecimal("1.5"));
        assertThat(c.optionalIntList("l")).contains(List.of(1, 2));
        assertThat(c.optionalStringList("l")).contains(List.of("1", "2"));
        assertThat(c.optionalInt("missing")).isEmpty();
    }

    @Test
    void errorsNameFormAndField() {
        ValidatorConfig c = new ValidatorConfig("signup", "age", "number", Map.of("min", "x", "flag", "maybe"));
        assertThatThrownBy(() -> c.optionalInt("min")).hasMessageContaining("field 'age'").hasMessageContaining("form 'signup'");
        assertThatThrownBy(() -> c.optionalBoolean("flag")).hasMessageContaining("true or false");
        assertThatThrownBy(() -> c.requireInt("nope")).hasMessageContaining("'nope' is required");
        assertThatThrownBy(() -> c.allowKeys("min")).hasMessageContaining("[flag]");
        assertThatThrownBy(() -> ValidatorConfig.of("t", Map.of("i", 1.5)).optionalInt("i")).hasMessageContaining("integer");
    }

    @Test
    void templateRendersKnownPlaceholdersAndKeepsUnknown() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("field", "age");
        vars.put("min", 18);
        vars.put("nul", null);
        assertThat(MessageTemplate.render("{field} >= {min}, {nul}, {unknown} $1", vars))
                .isEqualTo("age >= 18, null, {unknown} $1");
        assertThat(MessageTemplate.render("plain", vars)).isEqualTo("plain");
        assertThat(MessageTemplate.render(null, vars)).isNull();
    }
}
