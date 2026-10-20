package io.github.sumeetg13.validate4j.config;

import io.github.sumeetg13.validate4j.core.FieldRule;
import io.github.sumeetg13.validate4j.core.FormDefinition;
import io.github.sumeetg13.validate4j.core.Validator;
import io.github.sumeetg13.validate4j.spi.ValidatorFactory;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parses form definitions from YAML. Two layouts are accepted:
 *
 * <pre>
 * forms:
 *   signup:
 *     fields:
 *       name: { type: string, minLength: 2 }
 * </pre>
 *
 * or a single form per document:
 *
 * <pre>
 * form: signup
 * fields:
 *   name: { type: string, minLength: 2 }
 * </pre>
 *
 * Multi-document YAML (separated by {@code ---}) is supported for both layouts.
 * Parsing uses SnakeYAML's {@code SafeConstructor}, so no arbitrary classes are instantiated.
 */
public final class YamlFormLoader {

    public static final String KEY_FORMS = "forms";
    public static final String KEY_FORM = "form";
    public static final String KEY_FIELDS = "fields";
    public static final String KEY_TYPE = "type";
    public static final String KEY_REQUIRED = "required";
    public static final String KEY_MESSAGE = "message";

    private static final Set<String> RESERVED = Set.of(KEY_TYPE, KEY_REQUIRED, KEY_MESSAGE);

    private final ValidatorRegistry registry;

    public YamlFormLoader(ValidatorRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public List<FormDefinition> load(String yaml) {
        return load(new StringReader(yaml));
    }

    public List<FormDefinition> load(InputStream in) {
        Objects.requireNonNull(in, "in");
        return loadAll(newYaml().loadAll(in));
    }

    public List<FormDefinition> load(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return loadAll(newYaml().loadAll(reader));
    }

    private static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(options));
    }

    private List<FormDefinition> loadAll(Iterable<Object> documents) {
        List<FormDefinition> forms = new ArrayList<>();
        try {
            for (Object document : documents) {
                if (document == null) {
                    continue;
                }
                forms.addAll(parseDocument(document));
            }
        } catch (YAMLException e) {
            throw new InvalidConfigurationException("Malformed YAML: " + e.getMessage(), e);
        }
        return forms;
    }

    private List<FormDefinition> parseDocument(Object document) {
        Map<String, Object> root = asMap(document, "document root");
        if (root.containsKey(KEY_FORMS)) {
            Map<String, Object> formsNode = asMap(root.get(KEY_FORMS), "'forms'");
            List<FormDefinition> forms = new ArrayList<>(formsNode.size());
            for (Map.Entry<String, Object> e : formsNode.entrySet()) {
                Map<String, Object> body = asMap(e.getValue(), "form '" + e.getKey() + "'");
                forms.add(parseForm(e.getKey(), body));
            }
            return forms;
        }
        if (root.containsKey(KEY_FORM)) {
            String name = String.valueOf(root.get(KEY_FORM));
            return List.of(parseForm(name, root));
        }
        throw new InvalidConfigurationException(
                "YAML document must contain either a 'forms' map or a 'form' name with 'fields'. Found keys: "
                        + new TreeSet<>(root.keySet()));
    }

    private FormDefinition parseForm(String name, Map<String, Object> body) {
        Object fieldsNode = body.get(KEY_FIELDS);
        if (fieldsNode == null) {
            throw new InvalidConfigurationException("Form '" + name + "' has no 'fields'");
        }
        Map<String, Object> fields = asMap(fieldsNode, "'fields' of form '" + name + "'");
        List<FieldRule> rules = new ArrayList<>(fields.size());
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            rules.add(parseField(name, e.getKey(), e.getValue()));
        }
        return new FormDefinition(name, rules);
    }

    private FieldRule parseField(String formName, String fieldName, Object node) {
        Map<String, Object> raw = asMap(node, "field '" + fieldName + "' in form '" + formName + "'");
        Object typeNode = raw.get(KEY_TYPE);
        if (typeNode == null) {
            throw new InvalidConfigurationException(
                    "Field '" + fieldName + "' in form '" + formName + "' has no 'type'. Available types: "
                            + new TreeSet<>(registry.types()));
        }
        String type = String.valueOf(typeNode);
        ValidatorFactory factory = registry.find(type).orElseThrow(() -> new InvalidConfigurationException(
                "Unknown validator type '" + type + "' for field '" + fieldName + "' in form '" + formName
                        + "'. Available types: " + new TreeSet<>(registry.types())));

        Map<String, Object> options = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (!RESERVED.contains(e.getKey())) {
                options.put(e.getKey(), e.getValue());
            }
        }
        ValidatorConfig config = new ValidatorConfig(formName, fieldName, type, options);
        boolean required = parseRequired(config, raw.get(KEY_REQUIRED));
        String message = raw.get(KEY_MESSAGE) == null ? null : String.valueOf(raw.get(KEY_MESSAGE));

        Validator validator;
        try {
            validator = factory.create(config);
        } catch (InvalidConfigurationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw config.error("factory for type '" + type + "' failed: " + e.getMessage());
        }
        if (validator == null) {
            throw config.error("factory for type '" + type + "' returned null");
        }
        return new FieldRule(fieldName, type, required, message, validator, options);
    }

    private static boolean parseRequired(ValidatorConfig config, Object node) {
        if (node == null) {
            return false;
        }
        if (node instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(node).trim().toLowerCase();
        if (s.equals("true") || s.equals("false")) {
            return Boolean.parseBoolean(s);
        }
        throw config.error("'required' must be true or false but was '" + node + "'");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object node, String what) {
        if (!(node instanceof Map)) {
            throw new InvalidConfigurationException("Expected a mapping for " + what + " but found "
                    + (node == null ? "nothing" : node.getClass().getSimpleName()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> e : ((Map<Object, Object>) node).entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }
}
