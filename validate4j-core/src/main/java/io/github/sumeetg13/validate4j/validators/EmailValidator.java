package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * YAML type {@code email}. Options: {@code maxLength} (default 254). Uses a pragmatic pattern:
 * one {@code @}, a non-empty local part, and a domain with at least one dot and no whitespace.
 */
public final class EmailValidator extends AbstractValidator {

    public static final String TYPE = "email";
    public static final int DEFAULT_MAX_LENGTH = 254;

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    private final int maxLength;

    public EmailValidator(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        if (!(value instanceof CharSequence)) {
            return List.of(error(field, value, "email.type", "{field} must be text"));
        }
        String s = value.toString().trim();
        List<FieldError> errors = new ArrayList<>(2);
        if (s.length() > maxLength) {
            errors.add(error(field, value, "email.maxLength",
                    "{field} must be at most {maxLength} characters", "maxLength", maxLength));
        }
        if (s.startsWith(".") || s.contains("..") || s.contains(".@") || !EMAIL.matcher(s).matches()) {
            errors.add(error(field, value, "email.format", "{field} must be a valid email address"));
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
            config.allowKeys("maxLength");
            int max = config.optionalInt("maxLength").orElse(DEFAULT_MAX_LENGTH);
            if (max <= 0) {
                throw config.error("'maxLength' must be positive");
            }
            return new EmailValidator(max);
        }
    }
}
