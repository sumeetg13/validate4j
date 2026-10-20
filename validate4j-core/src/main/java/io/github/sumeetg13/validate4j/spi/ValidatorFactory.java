package io.github.sumeetg13.validate4j.spi;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.Validator;

/**
 * Service provider interface for validator types. Each factory owns one {@code type} name used
 * in YAML and builds an immutable {@link Validator} from that field's options.
 *
 * <p>Register custom factories through {@code META-INF/services/io.github.sumeetg13.validate4j.spi.ValidatorFactory},
 * through {@link ValidatorRegistry#register}, or as a Spring bean when using the starter.
 */
public interface ValidatorFactory {

    /** The YAML {@code type} this factory handles, for example {@code "string"}. Case-sensitive. */
    String type();

    /**
     * Build a validator. Implementations should call {@link ValidatorConfig#allowKeys} so typos in
     * YAML fail at startup instead of being silently ignored.
     *
     * @throws io.github.sumeetg13.validate4j.config.InvalidConfigurationException when the options are invalid
     */
    Validator create(ValidatorConfig config);
}
