/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.context.provider.stat.jvmgc;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.profiler.monitor.metric.gc.GarbageCollectorMetric;
import com.navercorp.pinpoint.profiler.monitor.metric.gc.GarbageCollectorMetricSnapshot;
import com.navercorp.pinpoint.profiler.monitor.metric.gc.JvmGcType;

/**
 * @author dawidmalina
 * @author HyunGil Jeong
 */
public class EmptyGarbageCollectorMetricProvider implements Provider<GarbageCollectorMetric> {

    @Inject
    public EmptyGarbageCollectorMetricProvider() {
    }

    @Override
    public GarbageCollectorMetric get() {
        return new GarbageCollectorMetric() {

            @Override
            public JvmGcType getGcType() {
                return JvmGcType.UNKNOWN;
            }

            @Override
            public GarbageCollectorMetricSnapshot getSnapshot() {
                return GarbageCollectorMetricSnapshot.EMPTY;
            }
        };
    }
}
