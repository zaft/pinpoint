package com.navercorp.pinpoint.bootstrap.graalvm;

import com.navercorp.pinpoint.bootstrap.context.TraceContext;

/**
 * Runtime-side handle to the live Pinpoint {@link TraceContext}, decoupled from
 * the woven interceptors.
 *
 * <p>Rationale (GraalVM native-image): interceptor instances are baked into the
 * image heap (they are reachable from the build-time {@code InterceptorRegistry}).
 * If an interceptor captured a {@code TraceContext} in a field, the whole Pinpoint
 * runtime graph — gRPC channels, executors/threads, Random — would become heap
 * reachable and either trip {@code DisallowedImageHeapObjectFeature} or get frozen
 * to meaningless builder-VM state. Interceptors therefore resolve the TraceContext
 * here, at call time, instead of holding it.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li><b>Build time</b>: nobody calls {@link #setTraceContext}; the field stays
 *       null, so no runtime object is reachable through this holder.</li>
 *   <li><b>Native runtime (milestone-3 stage ②/③)</b>: the freshly started profiler
 *       runtime calls {@link #setTraceContext} with the real, run-time-initialized
 *       {@code TraceContext}.</li>
 * </ul>
 *
 * <p>Stage ① (current): no runtime is wired yet, so {@link #getTraceContext} returns
 * null and interceptors simply skip trace recording — this validates that the woven
 * bytecode and the holder lookup execute correctly in the native binary without
 * pulling the runtime into the image heap.
 *
 * <p>Holds only the {@link TraceContext} interface (bootstrap-core, system CL); the
 * concrete impl lives in the profiler module loaded by the agent ClassLoader, so this
 * holder must not reference it directly. See [[feedback_heap_field_reset]].
 */
public final class PinpointRuntime {

    private static volatile TraceContext TRACE_CONTEXT;

    // Registered by build-mode premain (a stateless profiler-side impl → heap-safe).
    // Kept build-time-init: this reference is frozen into the image; TRACE_CONTEXT stays
    // null at build (setTraceContext is called only at runtime by booter.boot()).
    private static volatile RuntimeBooter BOOTER;
    private static volatile boolean started;
    private static final Object START_LOCK = new Object();

    private PinpointRuntime() {
    }

    public static void setTraceContext(TraceContext traceContext) {
        TRACE_CONTEXT = traceContext;
    }

    public static TraceContext getTraceContext() {
        return TRACE_CONTEXT;
    }

    /** Called once by build-mode premain to hand over the runtime-mode booter. */
    public static void setBooter(RuntimeBooter booter) {
        BOOTER = booter;
    }

    /**
     * Boots the runtime-mode Pinpoint context once, at native runtime. Idempotent.
     * Trigger from an early runtime hook (e.g. a Spring {@code ApplicationRunner}).
     * After this returns successfully, {@link #getTraceContext()} yields the live context.
     */
    public static void start() {
        if (started) {
            return;
        }
        synchronized (START_LOCK) {
            if (started) {
                return;
            }
            final RuntimeBooter booter = BOOTER;
            if (booter == null) {
                System.err.println("[PinpointRuntime] no RuntimeBooter registered "
                        + "— build-mode premain did not run, or registration was skipped");
                return;
            }
            booter.boot();          // builds runtime context, calls setTraceContext(...)
            started = true;
        }
    }
}
