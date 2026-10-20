package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.config.YamlFormLoader;
import io.github.sumeetg13.validate4j.core.FormDefinition;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link Validate4j} bean from every YAML file matching {@code validate4j.locations}.
 * Any {@link ValidatorFactory} bean is registered before the forms are loaded, so custom types can
 * be referenced from YAML. {@link Validate4jCustomizer} beans run last.
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

        ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        YamlFormLoader loader = new YamlFormLoader(registry);
        List<String> loadedFrom = new ArrayList<>();
        for (String location : properties.getLocations()) {
            for (Resource resource : resolve(resolver, location)) {
                try (InputStream in = resource.getInputStream()) {
                    List<FormDefinition> forms = loader.load(in);
                    builder.addForms(forms);
                    loadedFrom.add(resource.getDescription());
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read form definitions from " + resource, e);
                } catch (InvalidConfigurationException e) {
                    throw new InvalidConfigurationException(e.getMessage() + " (in " + resource.getDescription() + ")", e);
                }
            }
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
            log.info("validate4j loaded forms " + validate4j.formNames() + " from " + loadedFrom);
        }
        return validate4j;
    }

    @Bean
    @ConditionalOnMissingBean
    public FormInputConverter validate4jFormInputConverter() {
        return new DefaultFormInputConverter();
    }

    private static Resource[] resolve(ResourcePatternResolver resolver, String location) {
        try {
            Resource[] resources = resolver.getResources(location);
            List<Resource> existing = new ArrayList<>(resources.length);
            for (Resource r : resources) {
                if (r.exists()) {
                    existing.add(r);
                }
            }
            return existing.toArray(new Resource[0]);
        } catch (FileNotFoundException e) {
            // A wildcard under a directory that does not exist (e.g. "classpath:forms/*.yaml" with no forms/ dir).
            log.debug("validate4j location '" + location + "' matched nothing: " + e.getMessage());
            return new Resource[0];
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot resolve validate4j location '" + location + "'", e);
        }
    }
}
