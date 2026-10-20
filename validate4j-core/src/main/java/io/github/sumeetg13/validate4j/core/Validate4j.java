package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.YamlFormLoader;
import io.github.sumeetg13.validate4j.execution.DefaultExecutors;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Entry point: holds every loaded form and the shared execution settings.
 *
 * <pre>{@code
 * Validate4j v = Validate4j.builder()
 *         .loadYaml(Path.of("forms/signup.yaml"))
 *         .build();
 * ValidationResult result = v.validate("signup", Map.of("name", "Al", "age", 17));
 * }</pre>
 */
public final class Validate4j {

    private final Map<String, FormValidator> forms;
    private final ValidatorRegistry registry;

    private Validate4j(Map<String, FormValidator> forms, ValidatorRegistry registry) {
        this.forms = Collections.unmodifiableMap(new LinkedHashMap<>(forms));
        this.registry = registry;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @throws IllegalArgumentException when no form with that name was loaded */
    public FormValidator form(String name) {
        FormValidator validator = forms.get(name);
        if (validator == null) {
            throw new IllegalArgumentException("Unknown form '" + name + "'. Loaded forms: " + forms.keySet());
        }
        return validator;
    }

    public boolean hasForm(String name) {
        return forms.containsKey(name);
    }

    public Set<String> formNames() {
        return forms.keySet();
    }

    public ValidatorRegistry registry() {
        return registry;
    }

    public ValidationResult validate(String formName, Map<String, Object> input) {
        return form(formName).validate(input);
    }

    public ValidationResult validateOrThrow(String formName, Map<String, Object> input) {
        return form(formName).validateOrThrow(input);
    }

    public static final class Builder {

        private ValidatorRegistry registry = ValidatorRegistry.withDefaults();
        private Executor executor = DefaultExecutors.shared();
        private int parallelThreshold = FormValidator.DEFAULT_PARALLEL_THRESHOLD;
        private final Map<String, FormDefinition> definitions = new LinkedHashMap<>();

        private Builder() {
        }

        /** Replace the registry. Defaults to the built-ins plus anything found via {@code ServiceLoader}. */
        public Builder registry(ValidatorRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry");
            return this;
        }

        /** Add a custom validator type to the current registry. */
        public Builder register(ValidatorFactory factory) {
            registry.register(factory);
            return this;
        }

        /** Executor for parallel validation. Pass {@code null} to force sequential validation. */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /** Minimum number of fields before a form is validated in parallel. Default {@value FormValidator#DEFAULT_PARALLEL_THRESHOLD}. */
        public Builder parallelThreshold(int threshold) {
            this.parallelThreshold = threshold;
            return this;
        }

        public Builder addForm(FormDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            if (definitions.putIfAbsent(definition.name(), definition) != null) {
                throw new IllegalArgumentException("Form '" + definition.name() + "' is defined more than once");
            }
            return this;
        }

        public Builder addForms(Iterable<FormDefinition> forms) {
            for (FormDefinition form : forms) {
                addForm(form);
            }
            return this;
        }

        public Builder loadYaml(String yaml) {
            return addForms(new YamlFormLoader(registry).load(yaml));
        }

        public Builder loadYaml(Reader reader) {
            return addForms(new YamlFormLoader(registry).load(reader));
        }

        public Builder loadYaml(InputStream in) {
            return addForms(new YamlFormLoader(registry).load(in));
        }

        public Builder loadYaml(Path path) {
            try (InputStream in = Files.newInputStream(path)) {
                return loadYaml(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + path, e);
            }
        }

        public Builder loadYamlResource(String classpathLocation) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = Validate4j.class.getClassLoader();
            }
            String name = classpathLocation.startsWith("/") ? classpathLocation.substring(1) : classpathLocation;
            try (InputStream in = cl.getResourceAsStream(name)) {
                if (in == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + classpathLocation);
                }
                return loadYaml(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + classpathLocation, e);
            }
        }

        public List<FormDefinition> definitions() {
            return List.copyOf(definitions.values());
        }

        public Validate4j build() {
            Map<String, FormValidator> forms = new LinkedHashMap<>();
            for (FormDefinition definition : definitions.values()) {
                forms.put(definition.name(), new FormValidator(definition, executor, parallelThreshold));
            }
            return new Validate4j(forms, registry);
        }
    }
}
