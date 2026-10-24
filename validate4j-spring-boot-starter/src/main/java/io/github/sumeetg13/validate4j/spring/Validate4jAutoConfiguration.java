package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.core.ReloadListener;
import io.github.sumeetg13.validate4j.core.Validate4j;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.util.Set;

/**
 * Builds a {@link Validate4j} bean from every YAML file matching {@code validate4j.locations}.
 * Any {@link ValidatorFactory} bean is registered before the forms are loaded, so custom types can
 * be referenced from YAML. {@link Validate4jCustomizer} beans run last. With {@code validate4j.hot-reload=true}
 * form files on disk are watched and edits are applied without a restart.
 */
@AutoConfiguration
@EnableConfigurationProperties(Validate4jProperties.class)
@ConditionalOnProperty(prefix = "validate4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Validate4jAutoConfiguration {

    private static final Log log = LogFactory.getLog(Validate4jAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ValidatorRegistry validate4jRegistry(ObjectProvider<ValidatorFactory> factories) {
        ValidatorRegistry registry = ValidatorRegistry.withDefaults();
        factories.orderedStream().forEach(registry::register);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public Validate4j validate4j(Validate4jProperties properties, ValidatorRegistry registry,
                                 ResourceLoader resourceLoader, ObjectProvider<Validate4jCustomizer> customizers) {
        Validate4j.Builder builder = Validate4j.builder()
                .registry(registry)
                .parallelThreshold(properties.getParallelThreshold());

        ResourceLocationsFormSource source = new ResourceLocationsFormSource(
                ResourcePatternUtils.getResourcePatternResolver(resourceLoader), properties.getLocations());
        builder.source(source);
        if (properties.isHotReload()) {
            builder.hotReload(true).reloadListener(new LoggingReloadListener(source));
        }
        customizers.orderedStream().forEach(c -> c.customize(builder));

        Validate4j validate4j = builder.build();
        if (validate4j.formNames().isEmpty()) {
            String msg = "validate4j found no form definitions in " + properties.getLocations();
            if (properties.isFailOnMissingForms()) {
                throw new InvalidConfigurationException(msg);
            }
            log.warn(msg);
        } else {
            log.info("validate4j loaded forms " + validate4j.formNames() + " from " + source.loadedFrom());
        }
        if (validate4j.isWatching()) {
            log.info("validate4j hot reload is on: edits to the form files are applied without a restart");
        } else if (properties.isHotReload()) {
            log.warn("validate4j.hot-reload is on but none of " + properties.getLocations()
                    + " is a file on disk (resources inside a jar cannot be watched)");
        }
        return validate4j;
    }

    @Bean
    @ConditionalOnMissingBean
    public FormInputConverter validate4jFormInputConverter() {
        return new DefaultFormInputConverter();
    }

    private static final class LoggingReloadListener implements ReloadListener {

        private final ResourceLocationsFormSource source;

        LoggingReloadListener(ResourceLocationsFormSource source) {
            this.source = source;
        }

        @Override
        public void reloaded(Set<String> formNames) {
            log.info("validate4j reloaded forms " + formNames + " from " + source.loadedFrom());
        }

        @Override
        public void reloadFailed(RuntimeException error) {
            log.warn("validate4j reload failed, keeping previous forms: " + error.getMessage());
        }
    }
}
