package io.github.sumeetg13.validate4j.config;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders {@code {placeholder}} tokens from a variable map. Unknown placeholders are left untouched. */
public final class MessageTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_.]+)}");

    private MessageTemplate() {
    }

    public static String render(String template, Map<String, ?> vars) {
        if (template == null || template.indexOf('{') < 0) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder(template.length() + 16);
        while (m.find()) {
            String key = m.group(1);
            Object value = vars.get(key);
            String replacement = value == null && !vars.containsKey(key) ? m.group(0) : String.valueOf(value);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
