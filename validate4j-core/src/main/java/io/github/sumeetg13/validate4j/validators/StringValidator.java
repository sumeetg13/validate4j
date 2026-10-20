package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * YAML type {@code string}. Options: {@code minLength}, {@code maxLength}, {@code pattern}
 * (Java regex, matched against the whole value), {@code trim} (default {@code true}).
 */
public final class StringValidator extends AbstractValidator {

    public static final String TYPE = "string";

    private final Integer minLength;
    private final Integer maxLength;
    private final Pattern pattern;
    private final boolean trim;

    public StringValidator(Integer minLength, Integer maxLength, Pattern pattern, boolean trim) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.pattern = pattern;
        this.trim = trim;
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        if (!(value instanceof CharSequence)) {
            return List.of(error(field, value, "string.type", "{field} must be text"));
        }
        String s = trim ? value.toString().trim() : value.toString();
        List<FieldError> errors = new ArrayList<>(2);
        int length = s.codePointCount(0, s.length());
        if (minLength != null && length < minLength) {
            errors.add(error(field, value, "string.minLength",
                    "{field} must be at least {minLength} characters", "minLength", minLength));
        }
        if (maxLength != null && length > maxLength) {
            errors.add(error(field, value, "string.maxLength",
                    "{field} must be at most {maxLength} characters", "maxLength", maxLength));
        }
        if (pattern != null && !pattern.matcher(s).matches()) {
            errors.add(error(field, value, "string.pattern",
                    "{field} has an invalid format", "pattern", pattern.pattern()));
        }
        return errors;
    }

    public static final class Factory implements ValidatorFactory {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            config.allowKeys("minLength", "maxLength", "pattern", "trim");
            Integer min = config.optionalInt("minLength").orElse(null);
            Integer max = config.optionalInt("maxLength").orElse(null);
            if (min != null && min < 0) {
                throw config.error("'minLength' must be >= 0");
            }
            if (min != null && max != null && min > max) {
                throw config.error("'minLength' (" + min + ") must not exceed 'maxLength' (" + max + ")");
            }
            Pattern pattern = null;
            if (config.has("pattern")) {
                try {
                    pattern = Pattern.compile(config.requireString("pattern"));
                } catch (PatternSyntaxException e) {
                    throw config.error("'pattern' is not a valid regex: " + e.getDescription());
                }
            }
            return new StringValidator(min, max, pattern, config.booleanOr("trim", true));
        }
    }
}
