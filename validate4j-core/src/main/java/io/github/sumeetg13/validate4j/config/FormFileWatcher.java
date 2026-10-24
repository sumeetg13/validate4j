package io.github.sumeetg13.validate4j.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Watches form files with a {@link WatchService} and runs a callback after they change.
 *
 * <p>Editors often save in several steps (truncate + write, or write a temp file + rename), so
 * events are debounced: the callback runs once the files have been quiet for the debounce period.
 * The callback runs on a single daemon thread, never concurrently with itself.
 *
 * <p>On macOS the JDK has no native watch service and polls instead, so a change can take a few
 * seconds to be noticed.
 */
public final class FormFileWatcher implements AutoCloseable {

    public static final Duration DEFAULT_DEBOUNCE = Duration.ofMillis(200);

    private static final WatchEvent.Kind<?>[] KINDS = {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE};

    private final WatchService service;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    /** Directory → file names to react to. A directory watched as a whole maps to an empty set. */
    private final Map<Path, Set<Path>> filters = new HashMap<>();
    private final long debounceMillis;
    private final Runnable onChange;
    private final Thread thread;

    private FormFileWatcher(Collection<Path> paths, Duration debounce, Runnable onChange) throws IOException {
        this.debounceMillis = Math.max(1, debounce.toMillis());
        this.onChange = onChange;
        Map<Path, Set<Path>> files = new HashMap<>();
        for (Path p : paths) {
            Path path = p.toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                filters.put(path, Set.of());
            } else if (path.getParent() != null) {
                files.computeIfAbsent(path.getParent(), d -> new HashSet<>()).add(path.getFileName());
            }
        }
        files.forEach((dir, names) -> filters.putIfAbsent(dir, Set.copyOf(names)));
        FileSystem fs = filters.isEmpty() ? null : filters.keySet().iterator().next().getFileSystem();
        this.service = fs == null ? null : fs.newWatchService();
        try {
            for (Path dir : filters.keySet()) {
                if (Files.isDirectory(dir)) {
                    keys.put(register(dir), dir);
                }
            }
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
        this.thread = new Thread(this::run, "validate4j-file-watcher");
        thread.setDaemon(true);
    }

    /**
     * Starts watching {@code paths} (files or directories) and calls {@code onChange} after each burst
     * of changes. Paths that do not exist are ignored.
     */
    public static FormFileWatcher start(Collection<Path> paths, Duration debounce, Runnable onChange) {
        Objects.requireNonNull(paths, "paths");
        Objects.requireNonNull(debounce, "debounce");
        Objects.requireNonNull(onChange, "onChange");
        try {
            FormFileWatcher watcher = new FormFileWatcher(paths, debounce, onChange);
            if (watcher.service != null) {
                watcher.thread.start();
            }
            return watcher;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot watch " + paths, e);
        }
    }

    /** Directories currently being watched. */
    public Set<Path> watchedDirectories() {
        return Set.copyOf(keys.values());
    }

    @Override
    public void close() {
        if (service != null) {
            try {
                service.close();
            } catch (IOException ignored) {
                // nothing useful to do; the thread exits on ClosedWatchServiceException either way
            }
        }
    }

    private WatchKey register(Path dir) throws IOException {
        WatchEvent.Modifier[] modifiers = highSensitivity();
        if (modifiers.length > 0) {
            try {
                return dir.register(service, KINDS, modifiers);
            } catch (UnsupportedOperationException e) {
                // fall through
            }
        }
        return dir.register(service, KINDS);
    }

    private void run() {
        try {
            while (true) {
                boolean relevant = drain(service.take());
                WatchKey next;
                while ((next = service.poll(debounceMillis, TimeUnit.MILLISECONDS)) != null) {
                    relevant |= drain(next);
                }
                if (relevant) {
                    try {
                        onChange.run();
                    } catch (RuntimeException e) {
                        // the callback reports its own failures; keep watching
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException e) {
            // closed
        }
    }

    private boolean drain(WatchKey key) {
        Path dir = keys.get(key);
        boolean relevant = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                relevant = true;
            } else if (dir != null && event.context() instanceof Path name) {
                relevant |= matches(filters.get(dir), name);
            }
        }
        key.reset();
        return relevant;
    }

    private static boolean matches(Set<Path> names, Path name) {
        if (names == null) {
            return false;
        }
        if (!names.isEmpty()) {
            return names.contains(name);
        }
        // Whole directory: skip editor droppings such as .signup.yaml.swp and signup.yaml~
        String s = name.toString();
        return !s.startsWith(".") && !s.endsWith("~");
    }

    /**
     * The JDK's polling watch service (used on macOS) checks every 10 seconds by default.
     * {@code com.sun.nio.file.SensitivityWatchEventModifier.HIGH} lowers that to 2 seconds where it is
     * still available; it is looked up reflectively because it is non-standard and gone from newer JDKs.
     */
    private static WatchEvent.Modifier[] highSensitivity() {
        try {
            Class<?> type = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
            return new WatchEvent.Modifier[]{(WatchEvent.Modifier) type.getField("HIGH").get(null)};
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return new WatchEvent.Modifier[0];
        }
    }
}
