package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;

import java.util.List;

/** Custom type contributed as a Spring bean in tests. */
class UppercaseValidatorFactory implements ValidatorFactory {

    @Override
    public String type() {
        return "uppercase";
    }

    @Override
    public Validator create(ValidatorConfig config) {
        config.allowKeys();
        return (field, value, ctx) -> {
            String s = value.toString();
            return s.equals(s.toUpperCase()) ? List.of()
                    : List.of(new FieldError(field, "uppercase", field + " must be upper case"));
        };
    }
}
