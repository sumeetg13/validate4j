package io.github.sumeetg13.validate4j.core;

import java.util.Set;

/** Notified after an automatic reload triggered by a file change. */
public interface ReloadListener {

    /** The new forms are live. */
    default void reloaded(Set<String> formNames) {
    }

    /** Reading the changed files failed; the previous forms stay in use. */
    default void reloadFailed(RuntimeException error) {
    }

    /** Logs through {@link System.Logger}. */
    static ReloadListener logging() {
        System.Logger log = System.getLogger(Validate4j.class.getName());
        return new ReloadListener() {
            @Override
            public void reloaded(Set<String> formNames) {
                log.log(System.Logger.Level.INFO, "validate4j reloaded forms " + formNames);
            }

            @Override
            public void reloadFailed(RuntimeException error) {
                log.log(System.Logger.Level.WARNING, "validate4j reload failed, keeping previous forms: " + error.getMessage(), error);
            }
        };
    }
}
