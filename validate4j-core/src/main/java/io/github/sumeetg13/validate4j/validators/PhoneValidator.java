package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * YAML type {@code phone}. Options: {@code digits} (one allowed digit count or a list, e.g. {@code [10, 12]}),
 * {@code allowPlusPrefix} (default {@code true}), {@code allowSeparators} (default {@code true}: spaces,
 * dashes, dots and parentheses are ignored before counting digits).
 */
public final class PhoneValidator extends AbstractValidator {

    public static final String TYPE = "phone";

    private final Set<Integer> digits;
    private final boolean allowPlusPrefix;
    private final boolean allowSeparators;

    public PhoneValidator(Set<Integer> digits, boolean allowPlusPrefix, boolean allowSeparators) {
        this.digits = Set.copyOf(digits);
        this.allowPlusPrefix = allowPlusPrefix;
        this.allowSeparators = allowSeparators;
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        if (!(value instanceof CharSequence) && !(value instanceof Number)) {
            return List.of(error(field, value, "phone.type", "{field} must be text"));
        }
        String s = value.toString().trim();
        if (allowPlusPrefix && s.startsWith("+")) {
            s = s.substring(1);
        }
        StringBuilder onlyDigits = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                onlyDigits.append(c);
            } else if (allowSeparators && (c == ' ' || c == '-' || c == '.' || c == '(' || c == ')')) {
                continue;
            } else {
                return List.of(error(field, value, "phone.format", "{field} must contain only digits"));
            }
        }
        int count = onlyDigits.length();
        if (!digits.isEmpty() && !digits.contains(count)) {
            return List.of(error(field, value, "phone.digits",
                    "{field} must have {digits} digits", "digits", joinDigits()));
        }
        if (count == 0) {
            return List.of(error(field, value, "phone.format", "{field} must contain only digits"));
        }
        return List.of();
    }

    private String joinDigits() {
        List<Integer> sorted = new TreeSet<>(digits).stream().toList();
        if (sorted.size() == 1) {
            return String.valueOf(sorted.get(0));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append(i == sorted.size() - 1 ? " or " : ", ");
            }
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }

    public static final class Factory implements ValidatorFactory {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Validator create(ValidatorConfig config) {
            config.allowKeys("digits", "allowPlusPrefix", "allowSeparators");
            List<Integer> digits = config.optionalIntList("digits").orElse(List.of());
            for (Integer d : digits) {
                if (d <= 0) {
                    throw config.error("'digits' values must be positive but found " + d);
                }
            }
            return new PhoneValidator(new TreeSet<>(digits),
                    config.booleanOr("allowPlusPrefix", true),
                    config.booleanOr("allowSeparators", true));
        }
    }
}
