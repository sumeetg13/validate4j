package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** YAML type {@code enum}. Options: {@code values} (required list), {@code ignoreCase} (default {@code false}). */
public final class EnumValidator extends AbstractValidator {

    public static final String TYPE = "enum";

    private final List<String> values;
    private final Set<String> lookup;
    private final boolean ignoreCase;

    public EnumValidator(List<String> values, boolean ignoreCase) {
        this.values = List.copyOf(values);
        this.ignoreCase = ignoreCase;
        Set<String> set = new LinkedHashSet<>();
        for (String v : values) {
            set.add(ignoreCase ? v.toLowerCase(Locale.ROOT) : v);
        }
        this.lookup = Set.copyOf(set);
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        String s = value.toString().trim();
        String key = ignoreCase ? s.toLowerCase(Locale.ROOT) : s;
        if (lookup.contains(key)) {
            return List.of();
        }
        return List.of(error(field, value, "enum.values",
                "{field} must be one of {values}", "values", String.join(", ", values)));
    }

    public static final class Factory implements ValidatorFactory {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            config.allowKeys("values", "ignoreCase");
            List<String> values = config.optionalStringList("values")
                    .orElseThrow(() -> config.error("'values' is required for type 'enum'"));
            if (values.isEmpty()) {
                throw config.error("'values' must not be empty");
            }
            return new EnumValidator(values, config.booleanOr("ignoreCase", false));
        }
    }
}
