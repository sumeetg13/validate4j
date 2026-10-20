package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.MessageTemplate;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.Validator;

import java.util.HashMap;
import java.util.Map;

/** Small helper for built-ins: renders default messages with {@code {field}}, {@code {value}} and option placeholders. */
abstract class AbstractValidator implements Validator {

    protected FieldError error(String field, Object value, String code, String template, Object... keyValues) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("field", field);
        vars.put("value", value);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            vars.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return new FieldError(field, code, MessageTemplate.render(template, vars));
    }
}
