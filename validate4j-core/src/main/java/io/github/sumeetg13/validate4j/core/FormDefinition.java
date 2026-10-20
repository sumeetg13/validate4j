package io.github.sumeetg13.validate4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A named form with its ordered field rules. */
public record FormDefinition(String name, List<FieldRule> fields) {

    public FormDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fields, "fields");
        Set<String> seen = new LinkedHashSet<>();
        for (FieldRule rule : fields) {
            if (!seen.add(rule.field())) {
                throw new IllegalArgumentException("Duplicate field '" + rule.field() + "' in form '" + name + "'");
            }
        }
        fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    public List<String> fieldNames() {
        List<String> names = new ArrayList<>(fields.size());
        for (FieldRule rule : fields) {
            names.add(rule.field());
        }
        return names;
    }
}
