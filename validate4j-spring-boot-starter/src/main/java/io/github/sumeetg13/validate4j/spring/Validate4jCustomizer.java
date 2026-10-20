package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.core.Validate4j;

/**
 * Callback to tweak the {@link Validate4j.Builder} before it is built, for example to set a custom
 * executor or add forms programmatically. Register one or more as Spring beans.
 */
@FunctionalInterface
public interface Validate4jCustomizer {

    void customize(Validate4j.Builder builder);
}
