package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.core.Validate4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/** Registers the {@code @ValidateWith} advice and the ProblemDetail handler for Spring MVC apps. */
@AutoConfiguration(after = Validate4jAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestBodyAdvice.class)
@ConditionalOnBean(Validate4j.class)
@ConditionalOnProperty(prefix = "validate4j", name = "web-enabled", havingValue = "true", matchIfMissing = true)
public class Validate4jWebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ValidateWithRequestBodyAdvice validateWithRequestBodyAdvice(Validate4j validate4j, FormInputConverter converter) {
        return new ValidateWithRequestBodyAdvice(validate4j, converter);
    }

    @Bean
    @ConditionalOnMissingBean
    public FormValidationExceptionHandler formValidationExceptionHandler() {
        return new FormValidationExceptionHandler();
    }
}
