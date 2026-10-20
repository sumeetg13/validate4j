package io.github.sumeetg13.validate4j.spring;

import java.util.Map;

/**
 * Turns a request body object into the {@code Map<String, Object>} validate4j validates.
 * Override the default bean to support custom body types.
 */
@FunctionalInterface
public interface FormInputConverter {

    Map<String, Object> toInput(Object body);
}
