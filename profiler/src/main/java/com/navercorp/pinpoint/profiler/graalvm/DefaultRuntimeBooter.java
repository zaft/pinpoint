package com.navercorp.pinpoint.profiler.graalvm;

import com.google.inject.Module;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.navercorp.pinpoint.bootstrap.AgentOption;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfigLoader;
import com.navercorp.pinpoint.bootstrap.config.PropertyLoader;
import com.navercorp.pinpoint.bootstrap.config.PropertyLoaderFactory;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.graalvm.PinpointRuntime;
import com.navercorp.pinpoint.bootstrap.graalvm.RuntimeBooter;
import com.navercorp.pinpoint.common.util.OsEnvSimpleProperty;
import com.navercorp.pinpoint.common.util.PropertySnapshot;
import com.navercorp.pinpoint.common.util.SimpleProperty;
import com.navercorp.pinpoint.profiler.AgentInfoSender;
import com.navercorp.pinpoint.profiler.context.module.ModuleLifeCycle;
import com.navercorp.pinpoint.profiler.monitor.AgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockMonitor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

/**
 * Runtime-mode bootstrap (milestone-3 stage ②). Lives in the profiler module (agent
 * ClassLoader) because it needs Guice and the profiler context modules; the
 * {@link RuntimeBooter} interface it implements lives in bootstrap-core. Registered into
 * {@link PinpointRuntime} during build-mode premain; {@link #boot()} runs once at native
 * runtime, triggered by {@link PinpointRuntime#start()}.
 *
 * <p>Stateless on purpose: no instance fields, so the instance frozen into the image heap
 * at build time drags nothing in.
 *
 * <p>Unlike {@code DefaultApplicationContext}, this path does NOT touch
 * {@code Instrumentation} (native runtime has none — classes are already woven and frozen)
 * and does NOT bind the weaving spine; the {@code InterceptorRegistry} adaptor was bound
 * and baked into the image at build time, so {@code getInterceptor(id)} already resolves.
 */
public final class DefaultRuntimeBooter implements RuntimeBooter {

    private AgentDirectory agentDirectory;

    @Override
    public void boot() {
        // 1) runtime config from the agent dir + the binary's own system properties / env
        //    (NOT builder jvmArgs). Needs the deployed pinpoint-agent dir reachable at runtime:
        //    pass -Dpinpoint.agent.path=/path/to/pinpoint-agent to the native binary.
        agentDirectory = resolveAgentDir(resolveJavaAgentPath());
        final Properties properties = loadProperties();
        final ProfilerConfig config = ProfilerConfigLoader.load(properties);
        final AgentOption option = new DefaultAgentOption(
                System.getProperty("pinpoint.agentId"),
                System.getProperty("pinpoint.agentName"),
                System.getProperty("pinpoint.applicationName"),
                false,                                               // isContainer
                config,
                Collections.<String>emptyList(),                    // pluginJars: no weaving
                Collections.<String>emptyList());                   // bootstrapJarPaths

        // 2) runtime-mode module (full composition). DEVELOPMENT = lazy: only the singletons we
        //    fetch below + their deps are built, so the weaving spine (InstrumentEngine /
        //    ClassFileTransformer, which would need the null Instrumentation) is never instantiated.
        //    Build-mode drives the same modules with PRODUCTION via DefaultApplicationContext.
        final Module module = RuntimeModeModule.create(option);
        final Injector injector = Guice.createInjector(Stage.DEVELOPMENT, module);

        // 3) bring up gRPC transport (verified key from DefaultApplicationContext)
        final ModuleLifeCycle rpc =
                injector.getInstance(Key.get(ModuleLifeCycle.class, Names.named("RPC-MODULE")));
        rpc.start();

        // 4) inject the live TraceContext into the holder the interceptors read from
        final TraceContext traceContext = injector.getInstance(TraceContext.class);
        PinpointRuntime.setTraceContext(traceContext);

        // 5) start runtime threads (these classes clinit for the first time here → init-at-run-time)
        injector.getInstance(DeadlockMonitor.class).start();
        injector.getInstance(AgentInfoSender.class).start();
        injector.getInstance(AgentStatMonitor.class).start();

        // 6) shutdown hook registered now, at runtime (build-mode premain must NOT register it,
        //    or it would keep the context graph reachable from the image heap)
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    rpc.shutdown();
                } catch (Exception ignore) {
                    // best-effort
                }
            }
        }, "Pinpoint-runtime-shutdown"));

        System.out.println("[PinpointRuntime] runtime context booted, TraceContext wired: " + traceContext);
    }

    private Properties loadProperties() {

        final Path agentDirPath = agentDirectory.getAgentDirPath();
        final Path profilesPath = agentDirectory.getProfilesPath();
        final String[] profileDirs = agentDirectory.getProfileDirs();

        final SimpleProperty javaSystemProperty = copyJavaSystemProperty();
        final SimpleProperty osEnvProperty = copyOSEnvVariables();

        final PropertyLoaderFactory factory = new PropertyLoaderFactory(javaSystemProperty, osEnvProperty,
                agentDirPath, profilesPath, profileDirs);
        final PropertyLoader loader = factory.newPropertyLoader();
        final Properties properties = loader.load();

//        final String importPluginIds = StringUtils.defaultString(this.agentArgs.get(AgentParameter.IMPORT_PLUGIN), "");
//        properties.put(DefaultProfilerConfig.IMPORT_PLUGIN, importPluginIds);

        return properties;
    }
    private SimpleProperty copyJavaSystemProperty() {
        return new PropertySnapshot(System.getProperties());
    }


    private SimpleProperty copyOSEnvVariables() {
        return new OsEnvSimpleProperty(System.getenv());
    }

    private AgentDirectory resolveAgentDir(Path agentPath) {
        try {
            System.out.println("agentPath:" + agentPath);
            final AgentDirBaseClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver(agentPath);
            AgentDirectory agentDir = classPathResolver.resolve();
            return agentDir;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path resolveJavaAgentPath() {
        System.out.println(System.getProperties());
        System.out.println(System.getProperty("pinpoint.agent.path"));
        // TODO
        if (System.getProperty("pinpoint.agent.path") != null) {
            return Paths.get(System.getProperty("pinpoint.agent.path"));
        } else {
            return Paths.get(System.getProperty("user.dir"));
        }
    }
}
