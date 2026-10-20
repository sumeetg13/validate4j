package io.github.sumeetg13.validate4j.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Typed view of one field's YAML options, minus the reserved keys ({@code type}, {@code required},
 * {@code message}) which the loader consumes. Every accessor reports the form and field in its
 * error message so a misconfiguration is easy to locate.
 */
public final class ValidatorConfig {

    private final String formName;
    private final String fieldName;
    private final String type;
    private final Map<String, Object> options;

    public ValidatorConfig(String formName, String fieldName, String type, Map<String, Object> options) {
        this.formName = Objects.requireNonNull(formName, "formName");
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.type = Objects.requireNonNull(type, "type");
        this.options = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(options, "options")));
    }

    /** Convenience for tests and programmatic use. */
    public static ValidatorConfig of(String type, Map<String, Object> options) {
        return new ValidatorConfig("<programmatic>", "<field>", type, options);
    }

    public String formName() {
        return formName;
    }

    public String fieldName() {
        return fieldName;
    }

    public String type() {
        return type;
    }

    /** The raw options, unmodifiable. */
    public Map<String, Object> options() {
        return options;
    }

    public boolean has(String key) {
        return options.containsKey(key);
    }

    /** Fails if the options contain any key outside {@code allowed}. Call this first in every factory. */
    public ValidatorConfig allowKeys(String... allowed) {
        Set<String> allowedSet = Set.of(allowed);
        Set<String> unknown = new TreeSet<>();
        for (String key : options.keySet()) {
            if (!allowedSet.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            throw error("unknown option(s) " + unknown + " for type '" + type + "'. Allowed: " + new TreeSet<>(allowedSet));
        }
        return this;
    }

    public Optional<String> optionalString(String key) {
        Object v = options.get(key);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(v));
    }

    public String requireString(String key) {
        return optionalString(key).orElseThrow(() -> missing(key));
    }

    public Optional<Boolean> optionalBoolean(String key) {
        Object v = options.get(key);
        if (v == null) {
            return Optional.empty();
        }
        if (v instanceof Boolean b) {
            return Optional.of(b);
        }
        String s = String.valueOf(v).trim().toLowerCase();
        if (s.equals("true") || s.equals("false")) {
            return Optional.of(Boolean.parseBoolean(s));
        }
        throw error("option '" + key + "' must be true or false but was '" + v + "'");
    }

    public boolean booleanOr(String key, boolean defaultValue) {
        return optionalBoolean(key).orElse(defaultValue);
    }

    public Optional<BigDecimal> optionalNumber(String key) {
        Object v = options.get(key);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.of(toNumber(key, v));
    }

    public Optional<Integer> optionalInt(String key) {
        return optionalNumber(key).map(n -> {
            try {
                return n.intValueExact();
            } catch (ArithmeticException e) {
                throw error("option '" + key + "' must be an integer but was '" + n + "'");
            }
        });
    }

    public int requireInt(String key) {
        return optionalInt(key).orElseThrow(() -> missing(key));
    }

    public Optional<List<String>> optionalStringList(String key) {
        return optionalList(key).map(list -> {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return Collections.unmodifiableList(out);
        });
    }

    public Optional<List<Integer>> optionalIntList(String key) {
        return optionalList(key).map(list -> {
            List<Integer> out = new ArrayList<>(list.size());
            for (Object o : list) {
                try {
                    out.add(toNumber(key, o).intValueExact());
                } catch (ArithmeticException e) {
                    throw error("option '" + key + "' must contain integers but found '" + o + "'");
                }
            }
            return Collections.unmodifiableList(out);
        });
    }

    /** A list option. A scalar is treated as a one-element list for convenience. */
    public Optional<List<Object>> optionalList(String key) {
        Object v = options.get(key);
        if (v == null) {
            return Optional.empty();
        }
        if (v instanceof List<?> list) {
            return Optional.of(Collections.unmodifiableList(new ArrayList<>(list)));
        }
        return Optional.of(List.of(v));
    }

    public InvalidConfigurationException error(String detail) {
        return new InvalidConfigurationException(
                "Invalid configuration for field '" + fieldName + "' in form '" + formName + "': " + detail);
    }

    private InvalidConfigurationException missing(String key) {
        return error("option '" + key + "' is required for type '" + type + "'");
    }

    private BigDecimal toNumber(String key, Object v) {
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw error("option '" + key + "' must be a number but was '" + v + "'");
        }
    }
}
