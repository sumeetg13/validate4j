package io.github.sumeetg13.validate4j.execution;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * Picks a sensible default {@link Executor} for parallel field validation.
 *
 * <p>Validation tasks are short and CPU-bound, so on Java 21+ a virtual-thread-per-task executor
 * gives the cheapest hand-off; on Java 17 to 20 the common {@link ForkJoinPool} is used. The lookup
 * is reflective so the library still compiles with {@code --release 17}.
 */
public final class DefaultExecutors {

    private static final class Holder {
        static final Executor SHARED = create();
    }

    private DefaultExecutors() {
    }

    /** A lazily created, process-wide executor. */
    public static Executor shared() {
        return Holder.SHARED;
    }

    /** Creates a new executor using the best strategy available on this JVM. */
    public static Executor create() {
        Executor virtual = virtualThreadExecutor();
        return virtual != null ? virtual : ForkJoinPool.commonPool();
    }

    /** Whether this JVM offers virtual threads (Java 21+). */
    public static boolean virtualThreadsAvailable() {
        return virtualThreadExecutor() != null;
    }

    private static Executor virtualThreadExecutor() {
        try {
            Method m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return (ExecutorService) m.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
