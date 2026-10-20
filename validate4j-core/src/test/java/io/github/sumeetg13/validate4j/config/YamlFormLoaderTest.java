package io.github.sumeetg13.validate4j.config;

import io.github.sumeetg13.validate4j.core.FieldRule;
import io.github.sumeetg13.validate4j.core.FormDefinition;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlFormLoaderTest {

    private final YamlFormLoader loader = new YamlFormLoader(ValidatorRegistry.withDefaults());

    @Test
    void loadsMultiFormLayoutPreservingOrder() {
        List<FormDefinition> forms = loader.load("""
                forms:
                  signup:
                    fields:
                      name: { type: string, required: true, minLength: 2, message: "bad {field}" }
                      age: { type: number, min: 18 }
                  login:
                    fields:
                      email: { type: email }
                """);
        assertThat(forms).extracting(FormDefinition::name).containsExactly("signup", "login");
        FormDefinition signup = forms.get(0);
        assertThat(signup.fieldNames()).containsExactly("name", "age");
        FieldRule name = signup.fields().get(0);
        assertThat(name.required()).isTrue();
        assertThat(name.type()).isEqualTo("string");
        assertThat(name.message()).isEqualTo("bad {field}");
        assertThat(name.options()).containsOnlyKeys("minLength");
        assertThat(signup.fields().get(1).required()).isFalse();
    }

    @Test
    void loadsSingleFormLayoutAndMultipleDocuments() {
        List<FormDefinition> forms = loader.load("""
                form: a
                fields:
                  x: { type: string }
                ---
                form: b
                fields:
                  y: { type: number }
                """);
        assertThat(forms).extracting(FormDefinition::name).containsExactly("a", "b");
    }

    @Test
    void loadsFromClasspathResource() {
        List<FormDefinition> forms = loader.load(getClass().getResourceAsStream("/forms/signup.yaml"));
        assertThat(forms).hasSize(1);
        assertThat(forms.get(0).fieldNames()).containsExactly("name", "age", "phone", "email", "role", "birthDate");
    }

    @Test
    void failsFastOnUnknownType() {
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: { type: strng }"))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessageContaining("Unknown validator type 'strng'")
                .hasMessageContaining("field 'a'")
                .hasMessageContaining("string");
    }

    @Test
    void failsFastOnMissingTypeUnknownOptionAndBadRequired() {
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: { minLength: 2 }"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("has no 'type'");
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: { type: string, minLenght: 2 }"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("minLenght");
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: { type: string, required: maybe }"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("'required'");
    }

    @Test
    void failsOnStructuralProblems() {
        assertThatThrownBy(() -> loader.load("hello: world"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("'forms'");
        assertThatThrownBy(() -> loader.load("form: f"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("no 'fields'");
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: string"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Expected a mapping");
        assertThatThrownBy(() -> loader.load("form: f\nfields:\n  a: { type: string }\n  a: { type: number }"))
                .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Malformed YAML");
        assertThatThrownBy(() -> loader.load("form: [unclosed"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void doesNotInstantiateArbitraryClasses() {
        assertThatThrownBy(() -> loader.load("form: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader []]"))
                .isInstanceOf(InvalidConfigurationException.class);
    }
}
