package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.FormFileWatcher;
import io.github.sumeetg13.validate4j.config.FormSource;
import io.github.sumeetg13.validate4j.config.YamlFormLoader;
import io.github.sumeetg13.validate4j.execution.DefaultExecutors;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 *         .hotReload(true)   // optional: pick up edits to signup.yaml without a restart
 *         .build();
 * ValidationResult result = v.validate("signup", Map.of("name", "Al", "age", 17));
 * }</pre>
 *
 * <p>Forms are swapped atomically on {@link #reload()}: a validation already running finishes with
 * the forms it started with, and a failed reload leaves the current forms in place.
 */
public final class Validate4j implements AutoCloseable {

    private final ValidatorRegistry registry;
    private final List<FormSource> sources;
    private final Executor executor;
    private final int parallelThreshold;
    private final Object reloadLock = new Object();
    private volatile Map<String, FormValidator> forms;
    private volatile FormFileWatcher watcher;

    private Validate4j(Map<String, FormDefinition> definitions, List<FormSource> sources, ValidatorRegistry registry,
                       Executor executor, int parallelThreshold) {
        this.registry = registry;
        this.sources = List.copyOf(sources);
        this.executor = executor;
        this.parallelThreshold = parallelThreshold;
        this.forms = toValidators(definitions);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @throws IllegalArgumentException when no form with that name was loaded */
    public FormValidator form(String name) {
        Map<String, FormValidator> current = forms;
        FormValidator validator = current.get(name);
        if (validator == null) {
            throw new IllegalArgumentException("Unknown form '" + name + "'. Loaded forms: " + current.keySet());
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

    /**
     * Re-reads every source and swaps in the new forms. If any source fails the exception is thrown
     * and the current forms stay in use.
     *
     * @return the names of the forms now loaded
     */
    public Set<String> reload() {
        synchronized (reloadLock) {
            Map<String, FormDefinition> definitions = new LinkedHashMap<>();
            for (FormSource source : sources) {
                collect(definitions, source.load(registry));
            }
            Map<String, FormValidator> next = toValidators(definitions);
            forms = next;
            return next.keySet();
        }
    }

    /** Whether files are being watched for changes. */
    public boolean isWatching() {
        return watcher != null;
    }

    /** Stops watching files. Validation keeps working with the forms loaded last. */
    @Override
    public void close() {
        FormFileWatcher w = watcher;
        watcher = null;
        if (w != null) {
            w.close();
        }
    }

    private void watch(Duration debounce, ReloadListener listener) {
        Set<Path> paths = new LinkedHashSet<>();
        for (FormSource source : sources) {
            paths.addAll(source.watchedPaths());
        }
        if (paths.isEmpty()) {
            return;
        }
        watcher = FormFileWatcher.start(paths, debounce, () -> {
            Set<String> names;
            try {
                names = reload();
            } catch (RuntimeException e) {
                listener.reloadFailed(e);
                return;
            }
            listener.reloaded(names);
        });
    }

    private Map<String, FormValidator> toValidators(Map<String, FormDefinition> definitions) {
        Map<String, FormValidator> out = new LinkedHashMap<>();
        for (FormDefinition definition : definitions.values()) {
            out.put(definition.name(), new FormValidator(definition, executor, parallelThreshold));
        }
        return Collections.unmodifiableMap(out);
    }

    private static void collect(Map<String, FormDefinition> into, Iterable<FormDefinition> forms) {
        for (FormDefinition definition : forms) {
            Objects.requireNonNull(definition, "definition");
            if (into.putIfAbsent(definition.name(), definition) != null) {
                throw new IllegalArgumentException("Form '" + definition.name() + "' is defined more than once");
            }
        }
    }

    public static final class Builder {

        private ValidatorRegistry registry = ValidatorRegistry.withDefaults();
        private Executor executor = DefaultExecutors.shared();
        private int parallelThreshold = FormValidator.DEFAULT_PARALLEL_THRESHOLD;
        private boolean hotReload;
        private Duration debounce = FormFileWatcher.DEFAULT_DEBOUNCE;
        private ReloadListener reloadListener = ReloadListener.logging();
        private final Map<String, FormDefinition> definitions = new LinkedHashMap<>();
        private final List<FormSource> sources = new ArrayList<>();

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

        /**
         * Watch file-backed sources (such as {@link #loadYaml(Path)}) and {@link Validate4j#reload() reload}
         * when they change. Default {@code false}. Call {@link Validate4j#close()} to stop watching.
         */
        public Builder hotReload(boolean enabled) {
            this.hotReload = enabled;
            return this;
        }

        /** How long files must be quiet after a change before reloading. Default 200 ms. */
        public Builder hotReloadDebounce(Duration debounce) {
            this.debounce = Objects.requireNonNull(debounce, "debounce");
            return this;
        }

        /** Notified after each automatic reload. Defaults to logging through {@link System.Logger}. */
        public Builder reloadListener(ReloadListener listener) {
            this.reloadListener = Objects.requireNonNull(listener, "listener");
            return this;
        }

        public Builder addForm(FormDefinition definition) {
            return addForms(List.of(Objects.requireNonNull(definition, "definition")));
        }

        public Builder addForms(Iterable<FormDefinition> forms) {
            List<FormDefinition> copy = new ArrayList<>();
            forms.forEach(copy::add);
            return source(FormSource.of(copy));
        }

        /** Load forms from a source now, and again on every reload. */
        public Builder source(FormSource source) {
            Objects.requireNonNull(source, "source");
            collect(definitions, source.load(registry));
            sources.add(source);
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

        /** Load a YAML file. With {@link #hotReload(boolean)} on, edits to it are picked up automatically. */
        public Builder loadYaml(Path path) {
            return source(FormSource.yamlFile(path));
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
            Validate4j validate4j = new Validate4j(definitions, sources, registry, executor, parallelThreshold);
            if (hotReload) {
                validate4j.watch(debounce, reloadListener);
            }
            return validate4j;
        }
    }
}
