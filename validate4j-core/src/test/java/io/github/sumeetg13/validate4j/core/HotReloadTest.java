package io.github.sumeetg13.validate4j.core;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HotReloadTest {

    // The JDK polls instead of using native events on macOS, which can take ~10 s per change.
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path dir;

    @Test
    void reloadPicksUpEditsAndKeepsStaticForms() throws IOException {
        Path file = write("form: signup\nfields:\n  name: { type: string, minLength: 2 }");
        Validate4j v = Validate4j.builder()
                .loadYaml(file)
                .loadYaml("form: extra\nfields:\n  x: { type: string }")
                .build();
        assertThat(v.validate("signup", Map.of("name", "Ali")).isValid()).isTrue();

        write("form: signup\nfields:\n  name: { type: string, minLength: 5 }");
        assertThat(v.formNames()).containsExactly("signup", "extra"); // nothing changes until reload
        assertThat(v.reload()).containsExactly("signup", "extra");
        assertThat(v.validate("signup", Map.of("name", "Ali")).failedFields()).containsExactly("name");
    }

    @Test
    void failedReloadKeepsCurrentForms() throws IOException {
        Path file = write("form: signup\nfields:\n  name: { type: string }");
        Validate4j v = Validate4j.builder().loadYaml(file).build();

        write("form: signup\nfields:\n  name: { type: nope }");
        assertThatThrownBy(v::reload).isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("nope");
        assertThat(v.validate("signup", Map.of("name", "x")).isValid()).isTrue();

        Files.delete(file);
        assertThatThrownBy(v::reload).hasMessageContaining("Cannot read");
        assertThat(v.formNames()).containsExactly("signup");
    }

    @Test
    void reloadRejectsDuplicateFormsAcrossSources() throws IOException {
        Path file = write("form: a\nfields:\n  x: { type: string }");
        Validate4j v = Validate4j.builder().loadYaml(file).loadYaml("form: b\nfields:\n  x: { type: string }").build();
        write("form: b\nfields:\n  x: { type: string }");
        assertThatThrownBy(v::reload).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("more than once");
        assertThat(v.formNames()).containsExactly("a", "b");
    }

    @Test
    void hotReloadAppliesFileEditsAutomatically() throws Exception {
        Path file = write("form: signup\nfields:\n  name: { type: string, minLength: 2 }");
        List<Set<String>> reloaded = new CopyOnWriteArrayList<>();
        List<RuntimeException> failures = new CopyOnWriteArrayList<>();
        Validate4j v = Validate4j.builder()
                .loadYaml(file)
                .hotReload(true)
                .hotReloadDebounce(Duration.ofMillis(50))
                .reloadListener(new ReloadListener() {
                    @Override
                    public void reloaded(Set<String> formNames) {
                        reloaded.add(formNames);
                    }

                    @Override
                    public void reloadFailed(RuntimeException error) {
                        failures.add(error);
                    }
                })
                .build();
        try {
            assertThat(v.isWatching()).isTrue();

            write("form: signup\nfields:\n  name: { type: string, minLength: 5 }");
            await(() -> !v.validate("signup", Map.of("name", "Ali")).isValid());
            assertThat(reloaded).isNotEmpty();

            write("form: signup\nfields:\n  name: { type: broken }");
            await(() -> !failures.isEmpty());
            assertThat(failures.get(0)).hasMessageContaining("broken");
            assertThat(v.validate("signup", Map.of("name", "Ali")).failedFields()).containsExactly("name");
        } finally {
            v.close();
        }
        assertThat(v.isWatching()).isFalse();
    }

    @Test
    void hotReloadWithoutFileSourcesWatchesNothing() {
        try (Validate4j v = Validate4j.builder()
                .loadYaml("form: a\nfields:\n  x: { type: string }")
                .hotReload(true)
                .build()) {
            assertThat(v.isWatching()).isFalse();
        }
    }

    private Path write(String yaml) throws IOException {
        return Files.writeString(dir.resolve("forms.yaml"), yaml);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within " + TIMEOUT);
            }
            Thread.sleep(50);
        }
    }
}
