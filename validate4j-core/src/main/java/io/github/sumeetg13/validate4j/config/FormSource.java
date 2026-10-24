package io.github.sumeetg13.validate4j.config;

import io.github.sumeetg13.validate4j.core.FormDefinition;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Where form definitions come from. Every source is read once at build time and again on each
 * {@link io.github.sumeetg13.validate4j.core.Validate4j#reload() reload}, so a file-backed source
 * picks up edits made while the application runs.
 */
public interface FormSource {

    /** Reads the forms. Throws {@link InvalidConfigurationException} or {@link UncheckedIOException} on failure. */
    List<FormDefinition> load(ValidatorRegistry registry);

    /**
     * Files or directories whose changes should trigger a reload. A file means "this file only", a
     * directory means "anything in it". Empty for sources that never change.
     */
    default Set<Path> watchedPaths() {
        return Set.of();
    }

    /** A source that always returns the same, already-built forms. */
    static FormSource of(List<FormDefinition> forms) {
        List<FormDefinition> copy = List.copyOf(forms);
        return registry -> copy;
    }

    /** A YAML file read from disk on every load. */
    static FormSource yamlFile(Path path) {
        Objects.requireNonNull(path, "path");
        return new FormSource() {
            @Override
            public List<FormDefinition> load(ValidatorRegistry registry) {
                try (InputStream in = Files.newInputStream(path)) {
                    return new YamlFormLoader(registry).load(in);
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read " + path, e);
                }
            }

            @Override
            public Set<Path> watchedPaths() {
                return Set.of(path);
            }

            @Override
            public String toString() {
                return path.toString();
            }
        };
    }
}
