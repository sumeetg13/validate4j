package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.core.Validate4j;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class Validate4jAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Validate4jAutoConfiguration.class, Validate4jWebMvcAutoConfiguration.class));

    @Configuration(proxyBeanMethods = false)
    static class CustomTypeConfig {
        @Bean
        ValidatorFactory uppercase() {
            return new UppercaseValidatorFactory();
        }
    }

    @Test
    void loadsFormsFromDefaultLocationAndRegistersFactoryBeans() {
        runner.withUserConfiguration(CustomTypeConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(Validate4j.class).hasSingleBean(FormInputConverter.class);
            assertThat(ctx).doesNotHaveBean(ValidateWithRequestBodyAdvice.class); // not a web context
            Validate4j v = ctx.getBean(Validate4j.class);
            assertThat(v.formNames()).containsExactly("signup");
            assertThat(v.registry().types()).contains("uppercase", "string");
            assertThat(v.validate("signup", Map.of("name", "Al", "age", 20, "code", "abc")).failedFields())
                    .containsExactly("code");
        });
    }

    @Test
    void failsStartupWhenYamlReferencesUnknownType() {
        runner.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasRootCauseInstanceOf(InvalidConfigurationException.class)
                    .hasStackTraceContaining("Unknown validator type 'uppercase'")
                    .hasStackTraceContaining("forms.yaml");
        });
    }

    @Test
    void customLocationsAndCustomizerAndThreshold() {
        runner.withPropertyValues("validate4j.locations=classpath:validate4j/other-dir/login.yml",
                        "validate4j.parallel-threshold=1")
                .withBean(Validate4jCustomizer.class, () -> b -> b.loadYaml("form: extra\nfields:\n  x: { type: string }"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(Validate4j.class).formNames()).containsExactly("login", "extra");
                });
    }

    @Test
    void missingFormsWarnByDefaultAndFailWhenAsked() {
        runner.withPropertyValues("validate4j.locations=classpath:nowhere/*.yaml")
                .run(ctx -> assertThat(ctx.getBean(Validate4j.class).formNames()).isEmpty());
        runner.withPropertyValues("validate4j.locations=classpath:nowhere/*.yaml", "validate4j.fail-on-missing-forms=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("validate4j.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(Validate4j.class));
    }

    @Test
    void webContextRegistersAdviceAndHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Validate4jAutoConfiguration.class, Validate4jWebMvcAutoConfiguration.class))
                .withUserConfiguration(CustomTypeConfig.class)
                .run(ctx -> assertThat(ctx).hasSingleBean(ValidateWithRequestBodyAdvice.class)
                        .hasSingleBean(FormValidationExceptionHandler.class));
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Validate4jAutoConfiguration.class, Validate4jWebMvcAutoConfiguration.class))
                .withUserConfiguration(CustomTypeConfig.class)
                .withPropertyValues("validate4j.web-enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ValidateWithRequestBodyAdvice.class));
    }

    @Test
    void hotReloadAppliesEditsAndNewFiles(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.yaml"), "form: a\nfields:\n  x: { type: string, minLength: 2 }");
        runner.withPropertyValues("validate4j.locations=file:" + dir + "/*.yaml", "validate4j.hot-reload=true")
                .run(ctx -> {
                    Validate4j v = ctx.getBean(Validate4j.class);
                    assertThat(v.isWatching()).isTrue();
                    assertThat(v.validate("a", Map.of("x", "abc")).isValid()).isTrue();

                    Files.writeString(dir.resolve("a.yaml"), "form: a\nfields:\n  x: { type: string, minLength: 5 }");
                    Files.writeString(dir.resolve("b.yaml"), "form: b\nfields:\n  y: { type: string }");
                    await(() -> v.hasForm("b") && !v.validate("a", Map.of("x", "abc")).isValid());
                });
    }

    @Test
    void hotReloadIsOffByDefault() {
        runner.withUserConfiguration(CustomTypeConfig.class)
                .run(ctx -> assertThat(ctx.getBean(Validate4j.class).isWatching()).isFalse());
    }

    @Test
    void rootOfStripsTheWildcardPart() {
        assertThat(ResourceLocationsFormSource.rootOf("classpath*:validate4j/*.yaml")).isEqualTo("classpath*:validate4j/");
        assertThat(ResourceLocationsFormSource.rootOf("file:/etc/app/**/forms/*.yml")).isEqualTo("file:/etc/app/");
        assertThat(ResourceLocationsFormSource.rootOf("classpath:forms.yaml")).isEqualTo("classpath:");
        assertThat(ResourceLocationsFormSource.rootOf("file:/etc/app/forms.yaml")).isEqualTo("file:/etc/app/");
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within 30 s");
            }
            Thread.sleep(50);
        }
    }
}
