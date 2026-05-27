package com.navercorp.pinpoint.profiler.graalvm;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.navercorp.pinpoint.bootstrap.AgentOption;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.TransportModule;
import com.navercorp.pinpoint.profiler.context.module.*;
import com.navercorp.pinpoint.profiler.context.module.config.ConfigModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @description TODO
 * @date 2026/5/26 02:41
 */
public class RuntimeModeModule {

    public static Module create(AgentOption agentOption) {
        final Module config = new ConfigModule(agentOption);
        final Module pluginModule = new PluginModule();
        final Module applicationContextModule = new ApplicationContextModule();
        final Module rpcModule = newRpcModule(agentOption);
        final Module statsModule = new StatsModule();
        final Module thriftStatsModule = new ThriftStatsModule();

        return Modules.combine(config, pluginModule, applicationContextModule, rpcModule, statsModule, thriftStatsModule);
    }

    protected static Module newRpcModule(AgentOption agentOption) {
        ProfilerConfig profilerConfig = agentOption.getProfilerConfig();
        final TransportModule transportModule = profilerConfig.getTransportModule();
        if (TransportModule.GRPC == transportModule) {
//            logger.info("load GrpcModule");
            return new GrpcModule(profilerConfig);
        }
        if (TransportModule.THRIFT == transportModule) {
//            logger.info("load ThriftModule");
            return new ThriftModule(profilerConfig);
        }
//        logger.info("load ThriftModule");
        return new ThriftModule(profilerConfig);
    }
}
