package io.github.sumeetg13.validate4j.spring;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles {@code Map} bodies as-is, records via their components, and any other object via its
 * JavaBean getters. Property names become field names.
 */
public class DefaultFormInputConverter implements FormInputConverter {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toInput(Object body) {
        if (body == null) {
            return Map.of();
        }
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        if (body.getClass().isRecord()) {
            return fromRecord(body);
        }
        return fromBean(body);
    }

    private static Map<String, Object> fromRecord(Object record) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                out.put(component.getName(), component.getAccessor().invoke(record));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot read record component " + component.getName(), e);
            }
        }
        return out;
    }

    private static Map<String, Object> fromBean(Object bean) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        Map<String, Object> out = new LinkedHashMap<>();
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            if (pd.getReadMethod() != null && !"class".equals(pd.getName())) {
                out.put(pd.getName(), wrapper.getPropertyValue(pd.getName()));
            }
        }
        return out;
    }
}
