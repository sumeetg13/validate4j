package io.github.sumeetg13.validate4j.validators;

import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.config.ValidatorConfig;
import io.github.sumeetg13.validate4j.core.FieldError;
import io.github.sumeetg13.validate4j.core.ValidationContext;
import io.github.sumeetg13.validate4j.core.Validator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailEnumDateValidatorTest {

    private static final ValidationContext CTX = new ValidationContext("t", Map.of());

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Nested
    class Email {
        Validator v = new EmailValidator.Factory().create(ValidatorConfig.of("email", Map.of()));

        @Test
        void acceptsCommonAddresses() {
            assertThat(v.validate("e", "a.b+tag@example.co.uk", CTX)).isEmpty();
            assertThat(v.validate("e", " user@example.org ", CTX)).isEmpty();
        }

        @Test
        void rejectsMalformed() {
            for (String bad : List.of("plain", "@x.com", "a@b", "a@@b.com", "a b@c.com", ".a@b.com", "a..b@c.com", "a@-b.com")) {
                assertThat(codes(v.validate("e", bad, CTX))).as(bad).containsExactly("email.format");
            }
            assertThat(codes(v.validate("e", 5, CTX))).containsExactly("email.type");
        }

        @Test
        void enforcesMaxLength() {
            Validator small = new EmailValidator.Factory().create(ValidatorConfig.of("email", Map.of("maxLength", 10)));
            assertThat(codes(small.validate("e", "longname@example.com", CTX))).containsExactly("email.maxLength");
            assertThatThrownBy(() -> new EmailValidator.Factory().create(ValidatorConfig.of("email", Map.of("maxLength", 0))))
                    .isInstanceOf(InvalidConfigurationException.class);
        }
    }

    @Nested
    class Enum {
        @Test
        void matchesCaseSensitivelyByDefault() {
            Validator v = new EnumValidator.Factory().create(ValidatorConfig.of("enum", Map.of("values", List.of("admin", "user"))));
            assertThat(v.validate("role", "admin", CTX)).isEmpty();
            List<FieldError> errors = v.validate("role", "Admin", CTX);
            assertThat(codes(errors)).containsExactly("enum.values");
            assertThat(errors.get(0).message()).isEqualTo("role must be one of admin, user");
        }

        @Test
        void ignoreCase() {
            Validator v = new EnumValidator.Factory().create(ValidatorConfig.of("enum", Map.of("values", List.of("admin"), "ignoreCase", true)));
            assertThat(v.validate("role", "ADMIN", CTX)).isEmpty();
        }

        @Test
        void requiresValues() {
            assertThatThrownBy(() -> new EnumValidator.Factory().create(ValidatorConfig.of("enum", Map.of())))
                    .isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("values");
            assertThatThrownBy(() -> new EnumValidator.Factory().create(ValidatorConfig.of("enum", Map.of("values", List.of()))))
                    .isInstanceOf(InvalidConfigurationException.class);
        }
    }

    @Nested
    class Date {
        @Test
        void parsesIsoByDefaultAndRejectsImpossibleDates() {
            Validator v = new DateValidator.Factory().create(ValidatorConfig.of("date", Map.of()));
            assertThat(v.validate("d", "2024-02-29", CTX)).isEmpty();
            assertThat(v.validate("d", LocalDate.of(2020, 1, 1), CTX)).isEmpty();
            assertThat(codes(v.validate("d", "2023-02-29", CTX))).containsExactly("date.format");
            assertThat(codes(v.validate("d", "01/02/2023", CTX))).containsExactly("date.format");
            assertThat(codes(v.validate("d", 20230101, CTX))).containsExactly("date.type");
        }

        @Test
        void customFormatAndBounds() {
            Validator v = new DateValidator.Factory().create(ValidatorConfig.of("date",
                    Map.of("format", "dd/MM/yyyy", "after", "01/01/2000", "before", "01/01/2010")));
            assertThat(v.validate("d", "15/06/2005", CTX)).isEmpty();
            assertThat(codes(v.validate("d", "01/01/2000", CTX))).containsExactly("date.after");
            assertThat(codes(v.validate("d", "01/01/2010", CTX))).containsExactly("date.before");
            assertThat(v.validate("d", "01/01/2010", CTX).get(0).message()).isEqualTo("d must be before 01/01/2010");
        }

        @Test
        void todayKeyword() {
            Validator v = new DateValidator.Factory().create(ValidatorConfig.of("date", Map.of("before", "today")));
            assertThat(v.validate("d", LocalDate.now().minusDays(1), CTX)).isEmpty();
            assertThat(codes(v.validate("d", LocalDate.now(), CTX))).containsExactly("date.before");
        }

        @Test
        void rejectsBadConfig() {
            assertThatThrownBy(() -> new DateValidator.Factory().create(ValidatorConfig.of("date", Map.of("format", "QQQQQQ-x-zzzzzzz-[["))))
                    .isInstanceOf(InvalidConfigurationException.class);
            assertThatThrownBy(() -> new DateValidator.Factory().create(ValidatorConfig.of("date", Map.of("after", "nope"))))
                    .isInstanceOf(InvalidConfigurationException.class);
            assertThatThrownBy(() -> new DateValidator.Factory().create(ValidatorConfig.of("date", Map.of("after", "2020-01-01", "before", "2019-01-01"))))
                    .isInstanceOf(InvalidConfigurationException.class);
        }
    }
}
