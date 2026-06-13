package com.navercorp.pinpoint.profiler.context.provider.stat.filedescriptor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.profiler.monitor.metric.filedescriptor.FileDescriptorMetric;

/**
 * @description TODO
 * @date 2026/6/9 21:48
 */
public class EmptyFileDescriptorMetricProvider implements Provider<FileDescriptorMetric> {
    @Inject
    public EmptyFileDescriptorMetricProvider() {
    }

    @Override
    public FileDescriptorMetric get() {
        return FileDescriptorMetric.UNSUPPORTED_FILE_DESCRIPTOR_METRIC;
    }
}
