package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML type {@code number}. Options: {@code min}, {@code max} (both inclusive), {@code integer}
 * (default {@code false}). Accepts any {@link Number} or a numeric string, so it works for both
 * JSON bodies and HTML form posts.
 */
public final class NumberValidator extends AbstractValidator {

    public static final String TYPE = "number";

    private final BigDecimal min;
    private final BigDecimal max;
    private final boolean integer;

    public NumberValidator(BigDecimal min, BigDecimal max, boolean integer) {
        this.min = min;
        this.max = max;
        this.integer = integer;
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        BigDecimal n = parse(value);
        if (n == null) {
            return List.of(error(field, value, "number.type", "{field} must be a number"));
        }
        List<FieldError> errors = new ArrayList<>(2);
        if (integer && n.stripTrailingZeros().scale() > 0) {
            errors.add(error(field, value, "number.integer", "{field} must be a whole number"));
        }
        if (min != null && n.compareTo(min) < 0) {
            errors.add(error(field, value, "number.min", "{field} must be at least {min}", "min", min.toPlainString()));
        }
        if (max != null && n.compareTo(max) > 0) {
            errors.add(error(field, value, "number.max", "{field} must be at most {max}", "max", max.toPlainString()));
        }
        return errors;
    }

    static BigDecimal parse(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            return Double.isFinite(d) ? BigDecimal.valueOf(d) : null;
        }
        if (value instanceof Number num) {
            return new BigDecimal(num.toString());
        }
        if (value instanceof CharSequence cs) {
            try {
                return new BigDecimal(cs.toString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static final class Factory implements ValidatorFactory {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            config.allowKeys("min", "max", "integer");
            BigDecimal min = config.optionalNumber("min").orElse(null);
            BigDecimal max = config.optionalNumber("max").orElse(null);
            if (min != null && max != null && min.compareTo(max) > 0) {
                throw config.error("'min' (" + min + ") must not exceed 'max' (" + max + ")");
            }
            return new NumberValidator(min, max, config.booleanOr("integer", false));
        }
    }
}
