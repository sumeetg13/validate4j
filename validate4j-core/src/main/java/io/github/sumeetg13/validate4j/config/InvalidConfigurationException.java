package io.github.sumeetg13.validate4j.config;

/** Thrown at load time when a YAML form definition is malformed or references unknown options. */
public class InvalidConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidConfigurationException(String message) {
        super(message);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
