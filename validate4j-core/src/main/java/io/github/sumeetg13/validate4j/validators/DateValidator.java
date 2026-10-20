package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML type {@code date}. Options: {@code format} (a {@link DateTimeFormatter} pattern, default
 * ISO {@code yyyy-MM-dd}), {@code after} and {@code before} (exclusive bounds in the same format,
 * or the literal {@code today}). Accepts strings or {@link TemporalAccessor} values.
 */
public final class DateValidator extends AbstractValidator {

    public static final String TYPE = "date";
    public static final String TODAY = "today";

    private final DateTimeFormatter formatter;
    private final String formatDescription;
    private final LocalDate after;
    private final LocalDate before;
    private final boolean afterToday;
    private final boolean beforeToday;

    public DateValidator(DateTimeFormatter formatter, String formatDescription,
                         LocalDate after, boolean afterToday, LocalDate before, boolean beforeToday) {
        this.formatter = formatter;
        this.formatDescription = formatDescription;
        this.after = after;
        this.afterToday = afterToday;
        this.before = before;
        this.beforeToday = beforeToday;
    }

    @Override
    public List<FieldError> validate(String field, Object value, ValidationContext context) {
        LocalDate date;
        if (value instanceof LocalDate ld) {
            date = ld;
        } else if (value instanceof TemporalAccessor ta) {
            try {
                date = LocalDate.from(ta);
            } catch (RuntimeException e) {
                return List.of(error(field, value, "date.type", "{field} must be a date"));
            }
        } else if (value instanceof CharSequence cs) {
            try {
                date = LocalDate.parse(cs.toString().trim(), formatter);
            } catch (DateTimeParseException e) {
                return List.of(error(field, value, "date.format",
                        "{field} must be a date in the format {format}", "format", formatDescription));
            }
        } else {
            return List.of(error(field, value, "date.type", "{field} must be a date"));
        }

        LocalDate today = (afterToday || beforeToday) ? LocalDate.now() : null;
        LocalDate lower = afterToday ? today : after;
        LocalDate upper = beforeToday ? today : before;
        List<FieldError> errors = new ArrayList<>(2);
        if (lower != null && !date.isAfter(lower)) {
            errors.add(error(field, value, "date.after", "{field} must be after {after}",
                    "after", afterToday ? TODAY : lower.format(formatter)));
        }
        if (upper != null && !date.isBefore(upper)) {
            errors.add(error(field, value, "date.before", "{field} must be before {before}",
                    "before", beforeToday ? TODAY : upper.format(formatter)));
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
            config.allowKeys("format", "after", "before");
            String format = config.optionalString("format").orElse("uuuu-MM-dd");
            DateTimeFormatter formatter;
            try {
                // STRICT rejects impossible dates such as 2024-02-30. The pattern uses 'uuuu' because
                // strict resolving needs a proleptic year; 'yyyy' is accepted and rewritten.
                formatter = DateTimeFormatter.ofPattern(format.replace("yyyy", "uuuu"))
                        .withResolverStyle(ResolverStyle.STRICT);
            } catch (IllegalArgumentException e) {
                throw config.error("'format' is not a valid date pattern: " + e.getMessage());
            }
            LocalDate after = null;
            boolean afterToday = false;
            if (config.has("after")) {
                String s = config.requireString("after");
                if (TODAY.equalsIgnoreCase(s)) {
                    afterToday = true;
                } else {
                    after = parseBound(config, "after", s, formatter);
                }
            }
            LocalDate before = null;
            boolean beforeToday = false;
            if (config.has("before")) {
                String s = config.requireString("before");
                if (TODAY.equalsIgnoreCase(s)) {
                    beforeToday = true;
                } else {
                    before = parseBound(config, "before", s, formatter);
                }
            }
            if (after != null && before != null && !after.isBefore(before)) {
                throw config.error("'after' (" + after + ") must be earlier than 'before' (" + before + ")");
            }
            return new DateValidator(formatter, format, after, afterToday, before, beforeToday);
        }

        private static LocalDate parseBound(ValidatorConfig config, String key, String s, DateTimeFormatter f) {
            try {
                return LocalDate.parse(s.trim(), f);
            } catch (DateTimeParseException e) {
                throw config.error("'" + key + "' must be a date in the configured format or 'today' but was '" + s + "'");
            }
        }
    }
}
