package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Validate4jTest {

    @Test
    void endToEndFromClasspathYaml() {
        Validate4j v = Validate4j.builder().loadYamlResource("forms/signup.yaml").build();
        assertThat(v.formNames()).containsExactly("signup");

        Map<String, Object> good = Map.of("name", "Ada Lovelace", "age", 36, "phone", "+91 98765 43210",
                "email", "ada@example.com", "role", "ADMIN", "birthDate", "1815-12-10");
        assertThat(v.validate("signup", good).isValid()).isTrue();

        Map<String, Object> bad = new HashMap<>();
        bad.put("name", "A1");
        bad.put("age", "17.5");
        bad.put("phone", "12345");
        bad.put("email", "nope");
        bad.put("role", "guest");
        bad.put("birthDate", "2999-01-01");
        ValidationResult result = v.validate("signup", bad);
        assertThat(result.failedFields()).containsExactly("name", "age", "phone", "email", "role", "birthDate");
        assertThat(result.errorsFor("name")).extracting(FieldError::message).containsOnly("Name must be 2-50 letters");
        assertThat(result.errorsFor("age")).extracting(FieldError::code).containsExactly("number.integer", "number.min");
    }

    @Test
    void builtInsAreDiscoveredViaServiceLoader() {
        assertThat(ValidatorRegistry.withDefaults().types())
                .containsExactlyInAnyOrder("string", "number", "phone", "email", "enum", "date");
    }

    @Test
    void customValidatorTypeCanBeRegistered() {
        ValidatorFactory upper = new ValidatorFactory() {
            @Override
            public String type() {
                return "uppercase";
            }

            @Override
            public Validator create(ValidatorConfig config) {
                config.allowKeys();
                return (field, value, ctx) -> value.toString().equals(value.toString().toUpperCase())
                        ? List.of() : List.of(new FieldError(field, "uppercase", field + " must be upper case"));
            }
        };
        Validate4j v = Validate4j.builder()
                .register(upper)
                .loadYaml("form: f\nfields:\n  code: { type: uppercase, required: true }")
                .build();
        assertThat(v.validate("f", Map.of("code", "ABC")).isValid()).isTrue();
        assertThat(v.validate("f", Map.of("code", "abc")).errors()).extracting(FieldError::code).containsExactly("uppercase");
    }

    @Test
    void unknownFormAndDuplicateFormAreRejected() {
        Validate4j v = Validate4j.builder().loadYaml("form: a\nfields:\n  x: { type: string }").build();
        assertThatThrownBy(() -> v.form("b")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Loaded forms: [a]");
        assertThatThrownBy(() -> Validate4j.builder()
                .loadYaml("form: a\nfields:\n  x: { type: string }")
                .loadYaml("form: a\nfields:\n  y: { type: string }"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once");
        assertThatThrownBy(() -> Validate4j.builder().loadYamlResource("missing.yaml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
