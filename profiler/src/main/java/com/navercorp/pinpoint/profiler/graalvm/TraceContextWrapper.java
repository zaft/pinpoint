package com.navercorp.pinpoint.profiler.graalvm;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.graalvm.PinpointRuntime;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;

/**
 * @description TODO
 * @date 2026/6/9 01:16
 */
public class TraceContextWrapper implements TraceContext {

    public static final String PROPERTY_IMAGE_CODE_KEY = "org.graalvm.nativeimage.imagecode";
    public final static java.lang.String PROPERTY_IMAGE_CODE_VALUE_RUNTIME = "runtime";
    private final TraceContext delegate;

    public TraceContextWrapper(TraceContext delegate) {
        this.delegate = delegate;
    }

    private TraceContext getCurrentContext() {
        return PROPERTY_IMAGE_CODE_VALUE_RUNTIME.equals(System.getProperty(PROPERTY_IMAGE_CODE_KEY)) ? PinpointRuntime.getTraceContext() : delegate;
    }

    @Override
    public Trace currentTraceObject() {
        return getCurrentContext().currentTraceObject();
    }

    @Override
    public Trace currentRawTraceObject() {
        return getCurrentContext().currentRawTraceObject();
    }

    @Override
    public Trace continueTraceObject(TraceId traceId) {
        return getCurrentContext().continueTraceObject(traceId);
    }

    @Override
    public Trace continueTraceObject(Trace trace) {
        return getCurrentContext().continueTraceObject(trace);
    }

    @Override
    public Trace newTraceObject() {
        return getCurrentContext().newTraceObject();
    }

    @Override
    public Trace newTraceObject(String urlPath) {
        return getCurrentContext().newTraceObject(urlPath);
    }

    @Override
    public Trace newAsyncTraceObject() {
        return getCurrentContext().newAsyncTraceObject();
    }

    @Override
    public Trace newAsyncTraceObject(String urlPath) {
        return getCurrentContext().newAsyncTraceObject(urlPath);
    }

    @Override
    public Trace continueAsyncTraceObject(TraceId traceId) {
        return getCurrentContext().continueAsyncTraceObject(traceId);
    }

    @Override
    public Trace removeTraceObject() {
        return getCurrentContext().removeTraceObject();
    }

    @Override
    public Trace removeTraceObject(boolean closeDisableTrace) {
        return getCurrentContext().removeTraceObject(closeDisableTrace);
    }

    @Override
    public String getAgentId() {
        return getCurrentContext().getAgentId();
    }

    @Override
    public String getApplicationName() {
        return getCurrentContext().getApplicationName();
    }

    @Override
    public long getAgentStartTime() {
        return getCurrentContext().getAgentStartTime();
    }

    @Override
    public short getServerTypeCode() {
        return getCurrentContext().getServerTypeCode();
    }

    @Override
    public String getServerType() {
        return getCurrentContext().getServerType();
    }

    @Override
    public int cacheApi(MethodDescriptor methodDescriptor) {
        return getCurrentContext().cacheApi(methodDescriptor);
    }

    @Override
    public int cacheString(String value) {
        return getCurrentContext().cacheString(value);
    }

    @Override
    public ParsingResult parseSql(String sql) {
        return getCurrentContext().parseSql(sql);
    }

    @Override
    public boolean cacheSql(ParsingResult parsingResult) {
        return getCurrentContext().cacheSql(parsingResult);
    }

    @Override
    public TraceId createTraceId(String transactionId, long parentSpanId, long spanId, short flags) {
        return getCurrentContext().createTraceId(transactionId, parentSpanId, spanId, flags);
    }

    @Override
    public Trace disableSampling() {
        return getCurrentContext().disableSampling();
    }

    @Override
    public ProfilerConfig getProfilerConfig() {
        return getCurrentContext().getProfilerConfig();
    }

    @Override
    public ServerMetaDataHolder getServerMetaDataHolder() {
        return getCurrentContext().getServerMetaDataHolder();
    }

    @Override
    public JdbcContext getJdbcContext() {
        return getCurrentContext().getJdbcContext();
    }
}
