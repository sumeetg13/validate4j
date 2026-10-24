package io.github.sumeetg13.validate4j.spring;

import io.github.sumeetg13.validate4j.config.FormSource;
import io.github.sumeetg13.validate4j.config.InvalidConfigurationException;
import io.github.sumeetg13.validate4j.config.YamlFormLoader;
import io.github.sumeetg13.validate4j.core.FormDefinition;
import io.github.sumeetg13.validate4j.spi.ValidatorRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Forms from every resource matching {@code validate4j.locations}. Patterns are re-resolved on each
 * load, so files added to a watched directory are picked up too.
 */
final class ResourceLocationsFormSource implements FormSource {

    private static final Log log = LogFactory.getLog(ResourceLocationsFormSource.class);

    private final ResourcePatternResolver resolver;
    private final List<String> locations;
    private volatile List<String> loadedFrom = List.of();

    ResourceLocationsFormSource(ResourcePatternResolver resolver, List<String> locations) {
        this.resolver = resolver;
        this.locations = List.copyOf(locations);
    }

    @Override
    public List<FormDefinition> load(ValidatorRegistry registry) {
        YamlFormLoader loader = new YamlFormLoader(registry);
        List<FormDefinition> forms = new ArrayList<>();
        List<String> from = new ArrayList<>();
        for (String location : locations) {
            for (Resource resource : resolve(location)) {
                try (InputStream in = resource.getInputStream()) {
                    forms.addAll(loader.load(in));
                    from.add(resource.getDescription());
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read form definitions from " + resource, e);
                } catch (InvalidConfigurationException e) {
                    throw new InvalidConfigurationException(e.getMessage() + " (in " + resource.getDescription() + ")", e);
                }
            }
        }
        loadedFrom = List.copyOf(from);
        return forms;
    }

    /**
     * The directory of every matched file, plus the fixed part of each pattern (e.g. {@code validate4j/}
     * for {@code classpath*:validate4j/*.yaml}) so new files show up. Resources inside jars are skipped.
     */
    @Override
    public Set<Path> watchedPaths() {
        Set<Path> dirs = new LinkedHashSet<>();
        for (String location : locations) {
            for (Resource resource : resolve(location)) {
                File file = fileOf(resource);
                if (file != null && file.getParentFile() != null) {
                    dirs.add(file.getParentFile().toPath());
                }
            }
            for (Resource root : resolve(rootOf(location))) {
                File file = fileOf(root);
                if (file != null && file.isDirectory()) {
                    dirs.add(file.toPath());
                }
            }
        }
        return dirs;
    }

    List<String> loadedFrom() {
        return loadedFrom;
    }

    /** {@code classpath*:validate4j/*.yaml} → {@code classpath*:validate4j/} */
    static String rootOf(String location) {
        int prefixEnd = location.indexOf(':') + 1;
        int wildcard = location.length();
        for (char c : new char[]{'*', '?', '{'}) {
            int i = location.indexOf(c, prefixEnd);
            if (i >= 0) {
                wildcard = Math.min(wildcard, i);
            }
        }
        int slash = location.lastIndexOf('/', wildcard - 1);
        return slash < prefixEnd ? location.substring(0, prefixEnd) : location.substring(0, slash + 1);
    }

    private static File fileOf(Resource resource) {
        try {
            return resource.isFile() ? resource.getFile() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private Resource[] resolve(String location) {
        try {
            Resource[] resources = resolver.getResources(location);
            List<Resource> existing = new ArrayList<>(resources.length);
            for (Resource r : resources) {
                if (r.exists()) {
                    existing.add(r);
                }
            }
            return existing.toArray(new Resource[0]);
        } catch (FileNotFoundException e) {
            // A wildcard under a directory that does not exist (e.g. "classpath:forms/*.yaml" with no forms/ dir).
            log.debug("validate4j location '" + location + "' matched nothing: " + e.getMessage());
            return new Resource[0];
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot resolve validate4j location '" + location + "'", e);
        }
    }
}
