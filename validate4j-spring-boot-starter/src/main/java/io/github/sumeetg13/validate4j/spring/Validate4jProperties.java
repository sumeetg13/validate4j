package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.core.FormValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** {@code validate4j.*} configuration properties. */
@ConfigurationProperties(prefix = "validate4j")
public class Validate4jProperties {

    /** Whether validate4j auto-configuration is active. */
    private boolean enabled = true;

    /** Resource patterns to load form definitions from. Supports {@code classpath*:} and {@code file:} prefixes. */
    private List<String> locations = new ArrayList<>(List.of("classpath*:validate4j/*.yaml", "classpath*:validate4j/*.yml"));

    /** Minimum number of fields in a form before its fields are validated in parallel. */
    private int parallelThreshold = FormValidator.DEFAULT_PARALLEL_THRESHOLD;

    /** Whether to fail startup when no form definition file matches any location. */
    private boolean failOnMissingForms = false;

    /** Whether the web layer should register the {@code @ValidateWith} request body advice and ProblemDetail handler. */
    private boolean webEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public int getParallelThreshold() {
        return parallelThreshold;
    }

    public void setParallelThreshold(int parallelThreshold) {
        this.parallelThreshold = parallelThreshold;
    }

    public boolean isFailOnMissingForms() {
        return failOnMissingForms;
    }

    public void setFailOnMissingForms(boolean failOnMissingForms) {
        this.failOnMissingForms = failOnMissingForms;
    }

    public boolean isWebEnabled() {
        return webEnabled;
    }

    public void setWebEnabled(boolean webEnabled) {
        this.webEnabled = webEnabled;
    }
}
