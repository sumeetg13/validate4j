package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.core.Validate4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Map;

/** Runs validate4j on any {@code @RequestBody} parameter annotated with {@link ValidateWith}. */
@ControllerAdvice
public class ValidateWithRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final Validate4j validate4j;
    private final FormInputConverter converter;

    public ValidateWithRequestBodyAdvice(Validate4j validate4j, FormInputConverter converter) {
        this.validate4j = validate4j;
        this.converter = converter;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasParameterAnnotation(ValidateWith.class);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        ValidateWith annotation = parameter.getParameterAnnotation(ValidateWith.class);
        if (annotation != null) {
            Map<String, Object> input = converter.toInput(body);
            validate4j.validateOrThrow(annotation.value(), input);
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        ValidateWith annotation = parameter.getParameterAnnotation(ValidateWith.class);
        if (annotation != null) {
            validate4j.validateOrThrow(annotation.value(), Map.of());
        }
        return body;
    }
}
