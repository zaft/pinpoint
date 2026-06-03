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

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

/**
 * Runtime-mode bootstrap (milestone-3 stage ②). Lives in the profiler module (agent
 * ClassLoader) because it needs Guice and the profiler context modules; the
 * {@link RuntimeBooter} interface it implements lives in bootstrap-core. Registered into
 * {@link PinpointRuntime} during build-mode premain; {@link #boot(Instrumentation)} runs once at native
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

        // 0) re-declare `uses` for our named module. Build-time ModuleSupport.addUses(...) does
        //    not survive into the native image (GraalVM substitutes java.lang.Module; the uses
        //    Set is not serialized). Without this, ServiceLoader.load(...) from inside
        //    pinpoint.agent throws ServiceConfigurationError: does not declare `uses`.
        addRuntimeUses();
        // 1) runtime config from the agent dir + the binary's own system properties / env
        //    (NOT builder jvmArgs). Needs the deployed pinpoint-agent dir reachable at runtime:
        //    pass -Dpinpoint.agent.path=/path/to/pinpoint-agent to the native binary.
        agentDirectory = resolveAgentDir(resolveJavaAgentPath());
        final Properties properties = loadProperties();
        final ProfilerConfig config = ProfilerConfigLoader.load(properties);
        final AgentOption option = new DefaultAgentOption(
                new NoopInstrumentation(),
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

    // Mirrors ModuleSupport.prepareAgentModule(...) addUses calls. Reflection only — profiler is JDK 8 compiled.
    private void addRuntimeUses() {
        final String[] services = {
                "com.navercorp.pinpoint.common.trace.TraceMetadataProvider",
                "com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin",
                "com.navercorp.pinpoint.profiler.context.recorder.proxy.ProxyRequestParserProvider",
                "io.grpc.NameResolverProvider",
                "io.grpc.LoadBalancerProvider",
                "org.apache.logging.log4j.spi.Provider",
                "org.apache.logging.log4j.core.impl.Log4jProvider",
                "org.apache.logging.log4j.core.util.ContextDataProvider",
                "org.apache.logging.log4j.core.util.WatchEventService",
        };
        try {
            java.lang.reflect.Method getModule = Class.class.getMethod("getModule");
            Object agentModule = getModule.invoke(this.getClass());
            Class<?> moduleClass = Class.forName("java.lang.Module");
            java.lang.reflect.Method addUses = moduleClass.getMethod("addUses", Class.class);
            ClassLoader cl = this.getClass().getClassLoader();
            for (String svc : services) {
                try {
                    Class<?> svcClass = Class.forName(svc, false, cl);
                    addUses.invoke(agentModule, svcClass);
                } catch (Throwable t) {
                    System.err.println("[PinpointRuntime] addUses skipped: " + svc + " (" + t + ")");
                }
            }
        } catch (Throwable t) {
            System.err.println("[PinpointRuntime] addRuntimeUses failed: " + t);
            t.printStackTrace();
        }
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
        if (System.getProperty("pinpoint.agent.path") != null) {
            return Paths.get(System.getProperty("pinpoint.agent.path"));
        } else {
            return Paths.get(System.getProperty("user.dir"));
        }
    }
}
