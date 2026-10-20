package io.github.sumeetg13.validate4j.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/** Maps YAML {@code type} names to {@link ValidatorFactory} instances. Thread-safe. */
public final class ValidatorRegistry {

    private final Map<String, ValidatorFactory> factories = Collections.synchronizedMap(new LinkedHashMap<>());

    private ValidatorRegistry() {
    }

    /** An empty registry. Useful when you want full control over the available types. */
    public static ValidatorRegistry empty() {
        return new ValidatorRegistry();
    }

    /** Built-in validators plus every {@link ValidatorFactory} discoverable via {@link ServiceLoader}. */
    public static ValidatorRegistry withDefaults() {
        ValidatorRegistry registry = new ValidatorRegistry();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ValidatorRegistry.class.getClassLoader();
        }
        for (ValidatorFactory factory : ServiceLoader.load(ValidatorFactory.class, cl)) {
            registry.register(factory);
        }
        return registry;
    }

    /** Registers a factory, replacing any existing factory with the same type. */
    public ValidatorRegistry register(ValidatorFactory factory) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(factory.type(), "factory.type()");
        factories.put(factory.type(), factory);
        return this;
    }

    public Optional<ValidatorFactory> find(String type) {
        return Optional.ofNullable(factories.get(type));
    }

    public boolean contains(String type) {
        return factories.containsKey(type);
    }

    public Set<String> types() {
        synchronized (factories) {
            return Set.copyOf(factories.keySet());
        }
    }
}
