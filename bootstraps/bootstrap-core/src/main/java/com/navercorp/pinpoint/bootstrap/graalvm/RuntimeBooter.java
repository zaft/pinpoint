package com.navercorp.pinpoint.bootstrap.graalvm;

/**
 * Boots the runtime-mode Pinpoint ApplicationContext at native-image runtime and wires
 * the live {@link com.navercorp.pinpoint.bootstrap.context.TraceContext} into
 * {@link PinpointRuntime}.
 *
 * <p>Split deliberately, mirroring Pinpoint's {@code Agent}/{@code DefaultAgent} idiom:
 * this interface lives in bootstrap-core (boot ClassLoader, shared), while the
 * implementation lives in the profiler module (agent ClassLoader) because it needs
 * Guice and the profiler context modules. {@code Java9ClassLoader} self-loads only
 * {@code com.navercorp.pinpoint.profiler.*} (and other {@code ProfilerLibs} prefixes),
 * delegating {@code com.navercorp.pinpoint.bootstrap.*} to the parent — so the impl
 * cannot live under {@code bootstrap.*}.
 *
 * <p>A stateless implementation instance is registered into {@link PinpointRuntime}
 * during build-mode premain ({@code PinpointRuntime.setBooter(...)}); being field-less
 * it is heap-safe (drags nothing into the image heap). {@link #boot()} runs only at
 * native runtime, triggered by {@link PinpointRuntime#start()}.
 */
public interface RuntimeBooter {

    /**
     * Builds the runtime-mode ApplicationContext (real transport, no weaving) and calls
     * {@link PinpointRuntime#setTraceContext(com.navercorp.pinpoint.bootstrap.context.TraceContext)}.
     * Invoked once, at native runtime.
     */
    void boot();
}
